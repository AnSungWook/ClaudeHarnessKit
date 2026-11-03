# ADR-001 예시: 입력을 받은 자리부터 이름만 따라 읽히게

## 무엇을 보여 주나

API 로 배치 잡을 실행하는 창구다. 같은 잡이 겹치면 409, 잡이 끝날 때까지 기다렸다가(동기) 결과를 공통 응답 본문으로 돌려준다.
같은 요구를 두 모양으로 짰다. 바른 모양은 입구에서 이름만 따라 내려가도 실제 잡 실행까지 닿고, 어긋난 모양은 실행이 람다 두 겹 안쪽에 숨어 있다.

## 바른 모양

- `good/ReportJobController.java` - 입구. 다음에 열 자리가 `reportJobService.startReportJob` 으로 바로 보인다
- `good/ReportJobService.java` - 겹침 막기 → 잡 실행 → 정리가 한 메서드에 위에서 아래로
- `good/JobRunningFlag.java` - 겹침 막기 한 가지 일. `markStartedOrReject` 가 대상, 조건, 동작을 말한다

- `good/SpringBatchReportJobLauncher.java` - 바깥 포트 `ReportJobLauncher`(ADR-005)의 구현. `jobOperator.start` 와 Spring Batch 예외 감싸기(ADR-006)가 여기 있다
- `good/ReportJobConfig.java` - Job → Step → Reader/Processor/Writer 가 한 자리에 이름으로

흐름 글(PR 에 적는 모양, ADR-020):

```text
POST /jobs/report?targetKey=N → reportJobController.startReportJob(targetKey)
  reportJobService.startReportJob(targetKey)
    runningFlag.markStartedOrReject()            이미 돌고 있으면 ReportJobAlreadyRunningException → 공통 처리기가 409
    reportJobLauncher.runReportJob(targetKey)    SpringBatchReportJobLauncher
      jobOperator.start(reportJob, parameters)   기본 동기 실행기라 잡이 끝날 때 돌아온다
        reportJob → reportStep: pendingReportReader → reportSummaryProcessor → reportSummaryWriter
      잡을 못 띄우면 ReportJobLaunchFailedException → 공통 처리기가 503
    runningFlag.markFinished()                   finally
  CommonResponse.success(ReportJobResponse.of(result))
```

`ReportJobResult`, `ReportJobResponse`, 두 예외, reader·processor·writer 는 이 예시에 이름만 나온다.

## 어긋난 모양

- `bad/ReportJobController.java` - 입구가 `stream(runs.report(key), response)` 한 줄이라 무엇이 실행되는지 안 보이고, 실제 실행은 컨트롤러로 돌아와 `run.execute(...)` 에서 일어난다
- `bad/BatchRuns.java` - 실행하지 않고 "할 일" 을 람다로 싸서(`BatchRun.holding`) 돌려준다. `claim`, `holding` 은 구현 수단만 말한다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `stream(runs.report(key), response)` | `reportJobService.startReportJob(targetKey)` | 흐름의 단계를 나타내는 호출식이 맡은 일을 말한다 |
| `launcher.claim()`, `BatchRun.holding(...)`, `Slot` | `runningFlag.markStartedOrReject()`, `reportJobLauncher.runReportJob(...)` | 같은 줄 |
| 람다를 든 실행 객체를 돌려주고 위에서 실행 | 서비스 한 메서드에서 막기 → 실행 → 정리 | 이름으로 내용을 알 수 없는 실행 객체를 돌려줘 안을 뒤지게 하는 구조가 없다 |
| 줄 쓰기 클래스 셋(`RunLines`, `ResponseLines`, `RunProgress`) | 공통 응답 본문 하나(`CommonResponse`, ADR-023) | (위임은 되지만 이름이 하는 일을 말해야 한다) |
| 실제 잡 실행이 세 겹 안쪽 | 포트 한 번 건너 `jobOperator.start(...)` | 프레임워크의 익숙한 구조가 코드에서 보인다 |

포트(`ReportJobLauncher`)를 하나 건너는 것은 ADR-005 의 층 나눔 때문이다. 겹이 하나 늘어도 이름이 맡은 일을 말하므로 ADR-001 에 맞는다.

## 기계 검사

리뷰가 체크리스트로 본다. 이름이 맡은 일을 말하는지는 기계로 판정할 수 없다. 층 방향(ADR-005)은 `checks/` 의 ArchUnit 규칙이 본다.

## 공개 참고

- Spring Batch 공식 샘플 [`OwnersExportJobConfiguration`](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-samples/src/main/java/org/springframework/batch/samples/petclinic/OwnersExportJobConfiguration.java) - 잡 설정 하나에 reader, writer, step 이 이름으로 바로 보이는 모양. `good/ReportJobConfig.java` 가 따른 모양이다.
- [`spring-petclinic`](https://github.com/spring-projects/spring-petclinic) 의 `owner` 패키지(`OwnerController`) - 요청을 받아 처리까지 가는 웹 흐름이 한 클래스에서 보인다. 다만 컨트롤러가 저장소를 바로 부르므로 층 나눔은 ADR-005 를, `processCreationForm` 같은 `process` 이름 대신 메서드 이름은 이 ADR 을 따른다.
- [spring-modulith 예제 `OrderCompleted`](https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-examples/spring-modulith-example-full/src/main/java/example/order/OrderCompleted.java) - 이벤트 이름이 일어난 사실을 말하고, 언제 도는지가 `@ApplicationModuleListener` 선언에서 보인다. 다만 받는 메서드 이름이 `on(...)` 이라 하는 일을 말하지 않는다.
