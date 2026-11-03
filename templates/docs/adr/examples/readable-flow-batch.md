# 예시: API 로 배치 잡을 실행하는 흐름 (ADR-001)

[ADR-001](../ADR-001_readable-flow.md) 을 배치 실행 창구에 적용한 예다. 운영 도구(스케줄러, 셸 스크립트)가 API 를 불러 잡을 돌리고, 응답으로 시작 줄과 결과 줄을 받는 모양이다.
요구는 셋이다: 같은 잡이 겹치면 409, 잡이 끝날 때까지 응답을 붙듦(동기), 마지막 줄로 성공·실패를 알림.

## 흐름 글 (PR 에 적는 모양, ADR-020)

```text
POST /jobs/report?targetKey=N → reportJobController.startReportJob(targetKey, response)
  reportJobService.startReportJobAndWriteResult(targetKey, resultLines)
    runningFlag.markStartedOrReject()        이미 돌고 있으면 ReportJobAlreadyRunningException → 공통 처리기가 409
    resultLines.writeStart(targetKey)
    jobOperator.start(reportJob, parameters)  기본 동기 실행기라 잡이 끝날 때 돌아온다
      reportJob → reportStep: unsentReportReader → reportMailProcessor → reportMailWriter
    resultLines.writeResult(execution)        마지막 줄 "[report] end status=COMPLETED|FAILED"
      잡을 못 띄우면(JobExecutionException) resultLines.writeLaunchFailed(e)
    runningFlag.markFinished()               finally
```

## 읽기 어려운 모양 (이렇게 하지 않는다)

입구에서 무엇이 실행되는지 안 보이고, 실제 실행이 람다 두 겹 안쪽에 있다.

```java
@PostMapping("/jobs/report")
public void report(@RequestParam(required = false) Long targetKey, HttpServletResponse response) throws IOException {
    stream(runs.report(targetKey), response);           // runs 가 무엇을 돌려주는지 열어 봐야 안다
}

public BatchRun report(Long targetKey) {                // 실행하지 않고 "할 일" 을 싸서 돌려준다
    ClaimedRun claimed = launcher.claim(targetKey)       // claim: 무엇을 잡는지 이름에 없다
        .orElseThrow(() -> new AlreadyRunningException("report"));
    return BatchRun.holding(claimed.release(), lines ->  // holding: 무엇을 들고 있는지 이름에 없다
        claimed.run().apply(new RunProgress(lines)));     // 진짜 실행은 여기서도 아니고 Slot 안쪽
}

private static void stream(BatchRun run, HttpServletResponse response) throws IOException {
    try {
        run.execute(new RunLines(new ResponseLines(response.getWriter())));  // 위로 돌아와서야 실행된다
    } finally {
        run.release().run();
    }
}
```

## 따라 읽히는 모양

### 입구

```java
@RequiredArgsConstructor
@RestController
public class ReportJobController {

    private final ReportJobService reportJobService;

    @PostMapping(path = "/jobs/report", produces = "text/plain;charset=UTF-8")
    public void startReportJob(@RequestParam(required = false) Long targetKey, HttpServletResponse response)
        throws IOException {
        reportJobService.startReportJobAndWriteResult(targetKey, JobResultLines.of(response.getWriter()));
    }
}
```

### 실행 서비스 - 검사, 실행, 결과, 정리가 한 메서드에 순서대로

```java
@Slf4j
@RequiredArgsConstructor
@Service
public class ReportJobService {

    private final JobOperator jobOperator;
    private final Job reportJob;
    private final JobRunningFlag runningFlag;

    public void startReportJobAndWriteResult(Long targetKey, JobResultLines resultLines) {
        runningFlag.markStartedOrReject();
        try {
            resultLines.writeStart(targetKey);
            // 기본 실행기가 동기라 잡이 끝날 때 돌아온다. 비동기 실행기를 붙이면 결과 줄과 겹침 막기가 잡보다 먼저 끝난다.
            JobExecution execution = jobOperator.start(reportJob, reportParameters(targetKey));
            resultLines.writeResult(execution);
        } catch (JobExecutionException e) {
            // 실행 창구 경계다. 잡을 못 띄운 것도 응답의 마지막 줄(FAILED)로 알린다.
            log.error("Report job launch failed: targetKey={}", targetKey, e);
            resultLines.writeLaunchFailed(e);
        } finally {
            runningFlag.markFinished();
        }
    }

    private JobParameters reportParameters(Long targetKey) {
        // 요청마다 새 실행이 되게 고유 ID 를 넣는다. 같은 파라미터로 끝난 잡은 Spring Batch 가 다시 안 돌린다.
        JobParametersBuilder builder = new JobParametersBuilder().addString("runId", UUID.randomUUID().toString());
        if (targetKey != null) {
            builder.addLong("targetKey", targetKey);
        }
        return builder.toJobParameters();
    }
}
```

### 겹침 막기 - 한 가지 일만

```java
@Component
public class JobRunningFlag {

    // 배치는 서버 한 대에서 이 창구로만 돈다. 그래서 JVM 안의 표시로 겹침을 막는다(ADR-015).
    private final AtomicBoolean running = new AtomicBoolean();

    public void markStartedOrReject() {
        if (!running.compareAndSet(false, true)) {
            throw new ReportJobAlreadyRunningException();
        }
    }

    public void markFinished() {
        running.set(false);
    }
}

// 상태 공통 형제 ConflictException 을 상속해 공통 처리기가 409 로 바꾼다(ADR-006, ADR-023).
public class ReportJobAlreadyRunningException extends ConflictException {

    public ReportJobAlreadyRunningException() {
        super(ReportErrorCode.ALREADY_RUNNING);
    }
}
```

### 잡 정의 - 아는 모양 그대로 (Job → Step → Reader/Processor/Writer)

```java
@Configuration
public class ReportJobConfig {

    @Bean
    public Job reportJob(JobRepository jobRepository, Step reportStep) {
        return new JobBuilder("reportJob", jobRepository).start(reportStep).build();
    }

    @Bean
    public Step reportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
        UnsentReportReader unsentReportReader, ReportMailProcessor reportMailProcessor,
        ReportMailWriter reportMailWriter) {
        return new StepBuilder("reportStep", jobRepository)
            .<Report, ReportMail>chunk(100)
            .transactionManager(transactionManager)
            .reader(unsentReportReader)
            .processor(reportMailProcessor)
            .writer(reportMailWriter)
            .build();
    }
}
```

`JobResultLines` 는 시작 줄, 결과 줄, 기동 실패 줄을 쓰고 줄마다 flush 하는 클래스 하나다. 줄 쓰기를 위해 클래스를 여럿으로 나누지 않는다.
청크마다 진행 줄까지 보내야 하면 `ChunkListener` 하나를 스텝에 붙이고, 그 리스너가 무엇을 쓰는지 이름(`ReportProgressLineListener`)으로 말하게 한다. 이 예시는 거기까지 다루지 않는다.

## 무엇이 달라졌나

| 읽기 어려운 모양 | 따라 읽히는 모양 |
|---|---|
| `stream(runs.report(key), response)` - 무엇이 실행되는지 안 보임 | `reportJobService.startReportJobAndWriteResult(...)` - 이름이 하는 일을 말함 |
| `claim()`, `holding()`, `Slot` | `runningFlag.markStartedOrReject()` - 대상, 조건, 동작이 이름에 |
| 실행을 람다로 싸서 돌려주고 위에서 실행 | 서비스 한 메서드에서 검사 → 실행 → 결과 → 정리 |
| 줄 쓰기 클래스 넷(`RunLines`, `ResponseLines`, `RunProgress`, `Relay`) | `JobResultLines` 하나 |
| 실제 Job 실행이 세 겹 안쪽 | 서비스 본문에 `jobOperator.start(...)` 가 바로 보임 |

## 공개 참고 코드

- Spring Batch 공식 샘플 [`OwnersExportJobConfiguration`](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-samples/src/main/java/org/springframework/batch/samples/petclinic/OwnersExportJobConfiguration.java) - 잡 설정 하나에 reader, writer, step 이 이름으로 바로 보이는 모양. 이 예시의 `ReportJobConfig` 가 따른 모양이다.
- [`spring-petclinic`](https://github.com/spring-projects/spring-petclinic) 의 `owner` 패키지(`OwnerController`) - 요청을 받아 처리까지 가는 웹 흐름이 한 클래스에서 보이는 구조를 참고한다. 다만 컨트롤러가 저장소를 바로 부르므로 층 나눔은 ADR-005 를, `processCreationForm` 같은 `process` 이름 대신 메서드 이름은 이 ADR 을 따른다.
