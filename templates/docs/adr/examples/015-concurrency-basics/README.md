# ADR-015 예시: 공유 상태를 두지 않고, 스레드는 정해진 풀에서, 중단을 삼키지 않는다

[ADR-015](../../ADR-015_concurrency-basics.md) 를 배송 견적(여러 택배사를 동시에 부름)과 정산 작업 나누기(여러 서버)에 적용한 예다.

## 무엇을 보여 주나

서비스 빈은 무상태로 두고 요청 값은 지역 변수와 인자로만 넘긴다. 동시 호출은 크기와 큐 상한을 정한 실행기에 넘기고, 전체 대기에는 설정한 제한 시간을 두며, 기다리다 중단되면 중단 표시를 되살린다.
여러 서버가 같이 쓰는 DB 행은 JVM 잠금이 아니라 DB 행 잠금(`FOR UPDATE SKIP LOCKED`)으로 나눈다.

## 바른 모양

- `good/DeliveryQuoteProperties.java` - 실행기 크기, 큐 상한, 견적 전체 제한 시간을 든 설정 record(ADR-003). `@Positive`, 그리고 1ms 이상 int 밀리초 이하가 아닌 `timeout` 은 키 이름(`delivery.quote.timeout`)을 담아 기동 때 막는다.
- `good/DeliveryQuoteConfig.java` - 크기, 큐 상한, 스레드 이름을 정한 `quoteExecutor` 빈과, 검증된 제한 시간을 응용 층 타입 `DeliveryQuotePolicy(Duration timeout)`(`application/service` 의 record)으로 바꾼 빈. 큐가 찼거나 닫힌 실행기면 `AbortPolicy` 로 거부 예외를 던진다 - 조용히 버리는 정책은 그 작업의 `Future` 를 영영 안 끝나게 할 수 있다.
- `good/DeliveryQuoteService.java` - 가변 필드가 없다. 제한 시간은 `DeliveryQuotePolicy` 로 받는다. `supplyAsync(..., quoteExecutor)` 로 정한 실행기를 넘기고, `allOf(...).get(timeout)` 으로 전체 대기에 상한을 둔다. 갈래마다 사유 예외로 바꾼다.
  - 실행기 거부(`RejectedExecutionException`) → `DeliveryQuoteUnavailableException`
  - 제한 시간 초과 → `DeliveryQuoteTimeoutException`(견적 없음으로 알린다. 일부만 보여 주지 않는다)
  - 중단 → `Thread.currentThread().interrupt()` 뒤 `DeliveryQuoteUnavailableException`
  - 택배사 실패 → 원인을 단 `DeliveryQuoteUnavailableException`
- `good/JdbcSettlementTaskRepository.java` - `for update skip locked` 로 여러 서버가 서로 다른 대기 건을 가져간다. 상태 값은 enum 에서 매개변수로 묶고(ADR-003, ADR-013), DB 예외는 사유 예외로 감싸며(ADR-006), 결과는 `List.copyOf` 로 불변으로 돌려준다(ADR-009).

설정은 이렇게 둔다.

```yaml
delivery:
  quote:
    threads: 8
    queue-capacity: 100
    timeout: 3s
```

정산 서비스는 `@Transactional` 메서드 안에서 `findAndLockPending(limit)` 로 가져온 건을 처리 중으로 바꿔 저장하고 커밋한다. 처리(바깥 호출 포함)는 그 트랜잭션 밖에서 한다(ADR-014). 이 예시에서 서비스는 이름으로만 둔다.

## 어긋난 모양

- `bad/DeliveryQuoteService.java` - 싱글턴에 요청마다 바뀌는 `currentAddress` 필드, 실행기 없는 `supplyAsync`(공용 풀), 중단 표시를 되살리지 않는 `catch (InterruptedException e)`.
- `bad/SettlementTaskService.java` - 서버 한 대 보장 없이 `synchronized` 로 DB 대기 건 잡기를 지킨다. `@Transactional` 과 함께 쓰면 잠금이 커밋 전에 풀려 서버 한 대 안에서도 겹칠 수 있다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `private DeliveryAddress currentAddress;` 에 요청 값을 담음 | `getQuotes(address)` 의 인자와 람다로만 넘긴다 | 싱글턴 빈에 요청마다 바뀌는 가변 필드가 없다 |
| `CompletableFuture.supplyAsync(task)` | `supplyAsync(task, quoteExecutor)` + 크기·큐 상한을 정한 `ThreadPoolTaskExecutor` | `@Async`, `CompletableFuture` 가 정해진 실행기를 쓴다 |
| `catch (InterruptedException e) { throw ...; }` | `Thread.currentThread().interrupt();` 뒤 사유 예외 | `InterruptedException` 을 잡고 아무것도 안 하는 `catch` 가 없다(Decision: 중단 표시를 되살리고 끝내거나 던진다) |
| `synchronized startPendingTasks(...)` | `select ... for update skip locked` | 여러 서버가 같이 쓰는 데이터를 JVM 잠금으로 지키지 않는다 |
| `pendingQuote.get()` 을 끝없이 기다림 | `allOf(...).get(policy.timeout().toMillis(), MILLISECONDS)` | 바깥 호출에 시간 제한이 있다 |

- 택배사 HTTP 클라이언트의 읽기 타임아웃은 응답 바이트 사이의 대기 상한이라 견적 전체 대기의 상한이 되지 못한다. 그래서 견적 전체 제한 시간을 서비스에 따로 둔다.
- 제한 시간을 넘긴 뒤에도 남은 택배사 호출은 실행기 스레드에서 계속 돌 수 있다. 호출 하나의 전체 시간 상한은 HTTP 클라이언트의 응답 전체 타임아웃으로 ADR-018 이 맡는다.
- 남은 Future 를 취소하지 않는다. `CompletableFuture.cancel` 은 이미 도는 호출을 멈추지 못해 얻는 것이 없다.
- 서비스는 설정 층(`config`)을 import 하지 않는다. 바인딩 record `DeliveryQuoteProperties` 는 `config` 에 두고, 설정이 검증된 값을 `DeliveryQuotePolicy` 로 만들어 빈으로 넘긴다(ADR-003, ADR-005).
- `@Async` 도 같다. `@Async` 만 붙이면 기본 실행기로 가니 `@Async("quoteExecutor")` 처럼 이름 있는 실행기를 적는다. 이벤트 수신이면 `@Async` 다음 줄에 `@EventListener` 다(ADR-022).
- 공유 상태가 꼭 필요하면(JVM 안 캐시, 집계) 동시성 컬렉션이나 원자 타입을 쓰고 이유를 주석에 단다(ADR-010). 이 예시에는 그런 상태가 없다.
- `CarrierQuoteSource`, `SettlementTaskRepository`(포트), `DeliveryQuotePolicy`, `DeliveryQuote`, `DeliveryAddress`, `SettlementTask`, `SettlementTaskStatus`, `DeliveryQuoteUnavailableException`, `DeliveryQuoteTimeoutException`, `SettlementTaskStorageUnavailableException`(모두 `UnavailableException` 형제, ADR-006) 은 이름으로만 쓴다.

## 기계 검사

이 ADR 을 막는 기계 검사는 없다. 리뷰가 체크리스트로 본다.

## 공개 참고

- [spring-guides/gs-async-method `AsyncMethodApplication`](https://github.com/spring-guides/gs-async-method/blob/main/complete/src/main/java/com/example/asyncmethod/AsyncMethodApplication.java) - 크기와 큐 상한을 정한 실행기 빈. 같은 저장소의 서비스 파일은 ADR-018·011 을 어기므로 참고하지 않는다.
- [kagkarlsson/db-scheduler `PostgreSqlJdbcCustomization`](https://github.com/kagkarlsson/db-scheduler/blob/master/db-scheduler/src/main/java/com/github/kagkarlsson/scheduler/jdbc/PostgreSqlJdbcCustomization.java) - 여러 서버가 `FOR UPDATE SKIP LOCKED` 로 일을 나눠 가져가는 모양. 라이브러리 코드라 층 배치와 이름은 우리 ADR 의 기준이 아니다.
