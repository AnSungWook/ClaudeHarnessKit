# ADR-012 예시: 테스트는 범위를 나누고, 대역은 바깥 의존에만

[ADR-012](../../ADR-012_test-scope-and-doubles.md) 를 주문 도메인과 주문 생성 서비스의 단위 테스트에 적용한 예다.

## 무엇을 보여 주나

도메인과 서비스 단위 테스트는 컨텍스트를 띄우지 않고 `new` 로 만든다. 도메인 값은 진짜를 쓰고, 대역은 포트 뒤(저장소)와 시계에만 둔다.
목이 불렸는지가 아니라 돌려준 결과와 저장된 값을 리터럴 기대값으로 단언한다.

## 바른 모양

- `good/OrderTest.java` - 진짜 `Order`, `OrderLine` 으로 금액 합계, 상태 변경, 사유 예외를 본다. 기대값은 `6_000` 같은 리터럴.
- `good/InMemoryOrderRepository.java` - 저장소 포트의 가짜 구현(fake). 테스트가 저장된 값을 꺼내 단언할 수 있게 `savedOrders()` 를 둔다.
- `good/OrderPlacementServiceTest.java` - `new OrderPlacementService(가짜 저장소, Clock.fixed(...))`. 테스트 하나가 동작 하나를 보고, 이름이 그 동작을 말한다.

## 어긋난 모양

- `bad/OrderPlacementServiceTest.java` - 테스트 셋이 각각 다른 어긋남을 보인다.
  - `placeOrder()`: 값 객체 `OrderLine` 목, 결과 단언 없이 `verify(orders).save(any())` 만, 동작을 말하지 않는 이름.
  - `placeOrderReturnsTotalAmountOfLines()`: 결과는 단언하지만 기대값을 `1_500L * 2` 로 계산해 만든다.
  - `sendsReceiptAfterPlacingOrder()`: `Thread.sleep` 으로 비동기를 기다린다.
  - 클래스 전체: `@SpringBootTest` 로 단위 테스트.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@SpringBootTest` + `@Autowired` 서비스 | `new OrderPlacementService(orders, Clock.fixed(...))` | 도메인·서비스 단위 테스트가 애플리케이션 컨텍스트를 띄우지 않는다 |
| `mock(OrderLine.class)` 과 `when(line.amount())` | `new OrderLine(10L, 2, 1_500)` | 도메인 객체·값을 목으로 바꾸지 않는다 |
| `placeOrder()` 의 `verify(orders).save(any())` 만(결과 단언 없음) | `orders.savedOrders()` 의 `placedAt`, `result.totalAmount()` 단언 | `verify` 만 있고 결과 단언이 없는 테스트가 없다 |
| `placeOrderReturnsTotalAmountOfLines()` 의 `isEqualTo(1_500L * 2)` | `isEqualTo(3_000)` | 기대값을 대상 코드로 계산하지 않는다 |
| `Thread.sleep(2_000)` 뒤 `verify` | 조건을 기다리는 도구(Awaitility `await().atMost(...).untilAsserted(...)`)로, 통합 테스트에서 | 테스트에 `Thread.sleep` 이 없다 |
| `void placeOrder()` | `void placeOrderSavesOrderPlacedAtClockTime()` | (Decision: 테스트 하나는 한 가지 동작을 보고 이름이 그 동작을 말한다) |

- 테스트 이름은 영문 메서드 이름으로 동작을 말한다. `@DisplayName` 을 쓰는 저장소면 ADR-022 의 어노테이션 순서를 따른다.
- 가짜 저장소는 테스트 하나가 혼자 쓰므로 `ArrayList` 그대로 둔다. 이유는 그 필드 위 주석이 든다(ADR-010, ADR-015).
- 커밋 뒤 메일처럼 비동기로 도는 결과는 통합 테스트에서 진짜 이벤트를 흘려 기다린다. 이 예시는 단위 범위까지만 다루고, 그 모양은 아래 공개 참고(spring-modulith)가 보여 준다.
- `Order.place`, `PlaceOrderCommand`, `OrderPlacementResult`, `OrderRepository`, `ReceiptMailSender` 는 이름으로만 쓴다.

## 기계 검사

이 ADR 을 막는 기계 검사는 없다(견본 ArchUnit 은 테스트 코드를 검사하지 않는다). 리뷰가 체크리스트로 본다.

## 공개 참고

- [thombergs/buckpal `AccountTest`](https://github.com/thombergs/buckpal/blob/master/src/test/java/io/reflectoring/buckpal/application/domain/AccountTest.java) - 진짜 도메인 객체로 만들고 리터럴 기대값으로 단언한다.
  - 같은 저장소의 `SendMoneyServiceTest` 는 반례다. 도메인 객체를 목으로 바꾸고 `verify` 위주로 단언한다.
- [spring-modulith `OrderIntegrationTests`](https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-examples/spring-modulith-example-full/src/test/java/example/order/OrderIntegrationTests.java) - 비동기 이벤트를 `Scenario` 로 기다리고 결과를 단언한다. `Thread.sleep` 대신 조건을 기다리는 모양이다.
