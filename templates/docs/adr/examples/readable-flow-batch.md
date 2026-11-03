# 예시: API 로 배치 잡을 실행하는 흐름 (ADR-001)

[ADR-001](../ADR-001_readable-flow.md) 을 배치 실행 창구에 적용한 예다. 운영 도구(스케줄러, 셸 스크립트)가 API 를 불러 잡을 돌리고, 진행 상황을 응답으로 한 줄씩 받는 모양이다.
요구는 셋이다: 같은 잡이 겹치면 409, 진행 줄을 흘려보냄, 마지막 줄로 성공·실패를 알림.

## 흐름 글 (PR 에 적는 모양, ADR-020)

```text
POST /jobs/report?targetKey=N → reportJobController.startReportJob(targetKey, response)
  reportJobService.startReportJobAndWriteProgress(targetKey, progress)
    runningFlag.markStartedOrReject()       이미 돌고 있으면 ReportJobAlreadyRunningException(409)
    jobOperator.start(reportJob, parameters) 끝날 때까지 기다림(동기)
      reportJob → reportStep: unsentReportReader → reportMailProcessor → reportMailWriter
    progress.writeResult(execution)         마지막 줄 "[report] end status=COMPLETED|FAILED"
    runningFlag.markFinished()              finally
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
        reportJobService.startReportJobAndWriteProgress(targetKey, ProgressWriter.of(response.getWriter()));
    }
}
```

### 실행 서비스 - 검사, 실행, 정리가 한 메서드에 순서대로

```java
@RequiredArgsConstructor
@Service
public class ReportJobService {

    private final JobOperator jobOperator;
    private final Job reportJob;
    private final JobRunningFlag runningFlag;
    private final Clock clock;

    public void startReportJobAndWriteProgress(Long targetKey, ProgressWriter progress) {
        runningFlag.markStartedOrReject();
        try {
            progress.writeStart(targetKey);
            JobExecution execution = jobOperator.start(reportJob, reportParameters(targetKey));
            progress.writeResult(execution);
        } finally {
            runningFlag.markFinished();
        }
    }

    private JobParameters reportParameters(Long targetKey) {
        // 실행 시각을 넣어 매번 새 실행이 되게 한다. 같은 파라미터로 끝난 잡은 Spring Batch 가 다시 안 돌린다.
        JobParametersBuilder builder = new JobParametersBuilder().addLong("startedAt", clock.millis());
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

    // 배치는 서버 한 대에서만 돈다. 그래서 JVM 안의 표시로 겹침을 막는다(ADR-015).
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

`ProgressWriter` 는 시작 줄, 진행 줄, 결과 줄을 쓰는 클래스 하나다. 진행 줄을 위해 클래스를 여럿으로 나누지 않는다.

## 무엇이 달라졌나

| 읽기 어려운 모양 | 따라 읽히는 모양 |
|---|---|
| `stream(runs.report(key), response)` - 무엇이 실행되는지 안 보임 | `reportJobService.startReportJobAndWriteProgress(...)` - 이름이 하는 일을 말함 |
| `claim()`, `holding()`, `Slot` | `runningFlag.markStartedOrReject()` - 대상, 조건, 동작이 이름에 |
| 실행을 람다로 싸서 돌려주고 위에서 실행 | 서비스 한 메서드에서 검사 → 실행 → 결과 → 정리 |
| 진행 줄 클래스 넷(`RunLines`, `ResponseLines`, `RunProgress`, `Relay`) | `ProgressWriter` 하나 |
| 실제 Job 실행이 세 겹 안쪽 | 서비스 본문에 `jobOperator.start(...)` 가 바로 보임 |

## 공개 참고 코드

- Spring Batch 공식 샘플 [`OwnersExportJobConfiguration`](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-samples/src/main/java/org/springframework/batch/samples/petclinic/OwnersExportJobConfiguration.java) - 잡 설정 하나에 reader, writer, step 이 이름으로 바로 보이는 모양. 이 예시의 `ReportJobConfig` 가 따른 모양이다.
- [`spring-petclinic`](https://github.com/spring-projects/spring-petclinic) 의 `owner` 패키지(`OwnerController`) - 입구 메서드 이름과 본문만 읽어도 무엇을 하는지 보이는 웹 흐름. 다만 컨트롤러가 저장소를 바로 부르므로, 층 나눔은 이 하네스의 ADR-005 를 따른다.
