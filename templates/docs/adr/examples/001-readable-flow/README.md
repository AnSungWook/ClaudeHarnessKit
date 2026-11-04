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

## 서비스에 안 보이는 흐름: 이벤트, AOP

ADR-001 「서비스에 안 보이는 흐름」 절의 장면이다. 주문 접수(`order`)와 포인트(`point`) 두 기능이다.

### 바른 모양

- `good/OrderPlacementService.java` - 한 메서드에서 세 갈래를 가른다. **응답 전에 끝나야 하는** 재고 예약은 inventory 의 `port/in` 을 명시적으로 부르고, **나중에라도 끝나야 하는** 배송 요청은 같은 트랜잭션에 아웃박스로 책임만 남기고, **실패해도 되는** 영수증·포인트는 사실 이벤트(`OrderPlacedEvent`, 키만)를 낸다
- `good/ReceiptOnOrderPlacedListener.java` - 같은 기능의 커밋 뒤 실행. 커밋 뒤, 정해진 실행기에서 도는 것이 선언에 보이고, 리스너는 `SendReceiptUseCase` 에 넘기기만 한다
- `good/PointAccrualOnOrderPlacedListener.java` - 다른 기능의 이벤트를 받는다. 리스너는 `AccruePointUseCase` 에 넘기기만 하고, **실패 경로**는 유스케이스 구현(`PointAccrualService`, 같은 파일 아래)이 든다: 실패를 삼키지 않고 재적립 배치가 읽을 자리에 기록한다. 넘겨받은 회원 키로 권한을 갈음하지 않고 유스케이스가 다시 확인한다
- `good/RefundApprovalService.java` - 동작을 바꾸는 advice(`@PreAuthorize`, `@Transactional`)가 적용 메서드에 보인다
- `good/ServiceTimingAspect.java` - 표시 없이 패턴으로 거는 advice 는 관측만. 인자·반환값을 안 바꾸고, 예외를 삼키지 않고, 다시 실행하지 않는다

흐름 글:

```text
POST /orders → orderController.placeOrder(request)
  placeOrderUseCase.placeOrder(command)                 @Transactional
    reserveStock.reserveFor(items)                      응답 전에 끝나야 함 - 실패하면 주문 실패
    orders.save(Order.place(...))
    shippingRequests.recordFor(orderId)                 나중에라도 끝나야 함 - 아웃박스, 같은 트랜잭션
    events.publishEvent(OrderPlacedEvent(orderId, memberKey))
  커밋 뒤
    receiptExecutor: sendReceipt.sendFor(orderId)       실패하면 재발송 기록
    pointExecutor:   accruePoint.accrueFor(orderId, memberKey)   실패하면 재적립 기록
```

### 어긋난 모양

- `bad/OrderPlacementService.java` - 저장 한 줄 뒤 `OrderChanged` 이벤트 하나에 재고 차감, 배송 요청, 영수증을 다 싣는다. 영속 엔티티와 요청 객체를 그대로 싣는다
- `bad/OrderChangedListeners.java` - 서비스 폴더의 동기 리스너. 업무 순서를 `@Order` 에 숨기고, out 어댑터를 직접 부르고, 트랜잭션 안에서 바깥을 부르고, 실패를 삼킨다
- `bad/DiscountAdjustingAspect.java` - 표시 없이 서비스 전체에 걸어 업무 결과(할인)를 바꾼다

### 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| 재고 차감을 리스너 `@Order(1)` 로 | `reserveStock.reserveFor(...)` 명시적 호출 | 응답 전에 끝나야 하는 일은 서비스가 명시적으로 부른다 |
| 배송 요청을 리스너가 트랜잭션 안에서 바로 호출 | 같은 트랜잭션에 `shippingRequests.recordFor(...)` | 나중에라도 끝나야 하는 일은 그 책임이 업무 저장과 같은 트랜잭션에 남는다 |
| 리스너 안의 `catch (Exception e) {}` | 커밋 뒤·정해진 실행기에서 돌고, 유스케이스가 실패를 재시도 자리에 기록하고 로그 한 번 | 리스너 실패가 요청 결과를 뒤집지 않게 실행 시점과 실패 처리가 정해져 있다 |
| `@Order(1)`, `@Order(2)` | 순서가 필요한 일은 서비스의 호출 순서 | 꼭 지켜야 할 순서를 `@Order` 에 숨기지 않는다 |
| `stockJpaRepository.decrease(...)`, `mailClient.send(...)` | `sendReceipt.sendFor(...)`, `accruePoint.accrueFor(...)` | 리스너는 받는 기능의 `port/in` 에 넘기고 out 어댑터를 직접 부르지 않는다 |
| `new OrderChanged(order, command)` | `new OrderPlacedEvent(orderId, memberKey)` | 이벤트에 영속 엔티티, 요청 객체, 토큰이 없다 |
| `execution(* ..service..*(..))` 로 결과를 바꿈 | 메서드의 `@PreAuthorize`, 패턴 advice 는 `ServiceTimingAspect` 처럼 관측만 | 동작을 바꾸는 advice 는 적용 자리에 뜻이 읽히는 표시가 있다 |

`ShippingRequestOutbox`, 아웃박스 전달기, 재발송·재적립 배치, `SendReceiptUseCase` 구현은 이 예시에 이름만 나온다. 아웃박스의 전달·재시도·중복 처리 방식은 프로젝트 ADR 이 정한다.

## 기계 검사

리뷰가 체크리스트로 본다. 이름이 맡은 일을 말하는지는 기계로 판정할 수 없다. 층 방향(ADR-005)은 `checks/` 의 ArchUnit 규칙이 본다.
리스너·입구의 자리와 애스펙트 허용 목록은 `checks/` 의 흐름 관례 검사가 본다(`bad/OrderChangedListeners.java` 의 자리, `bad/DiscountAdjustingAspect.java` 가 관측 자리 밖이면 걸린다). 업무 순서를 `@Order` 에 숨겼는지, 실패를 삼켰는지, 무엇을 이벤트로 넘겼는지는 리뷰가 본다.

## 공개 참고

- Spring Batch 공식 샘플 [`OwnersExportJobConfiguration`](https://github.com/spring-projects/spring-batch/blob/main/spring-batch-samples/src/main/java/org/springframework/batch/samples/petclinic/OwnersExportJobConfiguration.java) - 잡 설정 하나에 reader, writer, step 이 이름으로 바로 보이는 모양. `good/ReportJobConfig.java` 가 따른 모양이다.
- [`spring-petclinic`](https://github.com/spring-projects/spring-petclinic) 의 `owner` 패키지(`OwnerController`) - 요청을 받아 처리까지 가는 웹 흐름이 한 클래스에서 보인다. 다만 컨트롤러가 저장소를 바로 부르므로 층 나눔은 ADR-005 를, `processCreationForm` 같은 `process` 이름 대신 메서드 이름은 이 ADR 을 따른다.
- [spring-modulith 예제 `OrderCompleted`](https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-examples/spring-modulith-example-full/src/main/java/example/order/OrderCompleted.java) - 이벤트 이름이 일어난 사실을 말하고, 언제 도는지가 `@ApplicationModuleListener` 선언에서 보인다. 다만 받는 메서드 이름이 `on(...)` 이라 하는 일을 말하지 않는다.
