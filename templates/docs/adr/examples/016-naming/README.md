# ADR-016 예시: 이름은 뜻을 드러내고 업무 용어 하나로 통일한다

[ADR-016](../../ADR-016_naming.md) 을 주문 조회와 취소에 적용한 예다.

## 무엇을 보여 주나

같은 일(주문 하나 조회, 조건 조회, 취소)을 하는 코드를 두 가지 이름으로 짰다.
바른 모양은 저장소 포트 읽기는 `find`(`Optional`), 서비스 읽기는 `get`(없으면 사유 예외), 쓰기는 `save` 와 업무 동사다. "취소" 는 `cancel` 한 낱말로 끝까지 부른다(`OrderCancelCommand`, `cancelOrder`, `order.cancel()`, `OrderStatus.CANCELED`, `OrderNotCancellableException`).

## 바른 모양

- [`good/Order.java`](good/Order.java) - 도메인 값. 상태를 바꾸는 업무 동작이 `cancel()`, 불리언이 `isPaid()`, `hasCoupon()` 으로 참·거짓으로 읽힌다. 담은 것을 말하는 칸 이름(`paidAmount`).
- [`good/OrderRepository.java`](good/OrderRepository.java) - 저장소 포트. 읽기 `findById`(`Optional`), 조건 객체를 받는 `findAllBy(condition)`, 쓰기 `save`.
- [`good/OrderQueryService.java`](good/OrderQueryService.java) - 읽기 서비스. `getOrder` 는 없으면 `OrderNotFoundException`, 돌려주는 값은 용도 접미사 `OrderResult`.
- [`good/OrderCancellationService.java`](good/OrderCancellationService.java) - 유스케이스 서비스 `{대상}{업무}Service`. 쓰기는 업무 동사 `cancelOrder`, 입력은 `OrderCancelCommand`.

## 어긋난 모양

- [`bad/OrderManager.java`](bad/OrderManager.java) - 같은 세 가지 일을 `Manager` 하나가 한다. 서비스 읽기가 `findOrder`, 변수 `data`, 약어 `ordRepo`, `orderList`, `OrderDto` 하나를 모든 층에 쓰고, 같은 취소를 `revokeOrder` 로 부른다.
- [`bad/IOrderRepository.java`](bad/IOrderRepository.java) - 포트 이름에 `I` 접두어, 읽기가 `getOrder`, 쓰기가 `insertOrder`, 조건 조회가 `selectOrderList(OrderDto)`.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@Service class OrderManager` | `OrderQueryService`, `OrderCancellationService` 로 나눔 | `@Service` 클래스가 `Service` 로 끝난다 / `Manager`… 로 끝나는 새 타입이 없다 |
| 서비스 `findOrder(id)` | 서비스 `getOrder(orderId)`, 없으면 사유 예외 | 포트 읽기는 `find`, 서비스 읽기는 `get` 이다 |
| 포트 `getOrder(id)`, `insertOrder(order)` | 포트 `findById(orderId)`, `save(order)` | 포트 읽기는 `find` / 포트 쓰기는 `save`/`update`/`delete` |
| `selectOrderList(OrderDto param)` | `findAllBy(OrderSearchCondition condition)` | 층 사이 값이 용도를 말한다 / 조건 객체는 `...By` |
| `OrderDto` 하나를 요청, 응답, 반환에 | `OrderCancelCommand`, `OrderResult`(응답은 `OrderResponse`) | 층 사이 값이 `Request`/`Response`/`Command`/`Condition`/`Result` 로 용도를 말한다 |
| `Order data`, `ordRepo`, `orderList` | `canceledOrder`, `orders` | 뜻 없는 이름이 없다 / 새 약어가 없다 / 이름에 타입 표시가 없다 |
| `IOrderRepository` | `OrderRepository`(구현은 `JpaOrderRepository` 처럼 기술 이름) | 이름에 `I` 접두어, `Impl` 이 없다 |
| `revokeOrder` 와 `order.cancel()` | `cancelOrder` 와 `order.cancel()` | 같은 개념에 두 낱말을 쓰지 않는다 |

## 기계 검사

[`checks/AdrArchitectureTest.java`](../../checks/README.md) 가 둘을 막는다.

- `servicesEndWithService` - `@Service` 인데 이름이 `Service` 로 안 끝나는 `OrderManager`.
- `noVagueTypeNames` - `Manager`, `Dto` 로 끝나는 `OrderManager`, `OrderDto`.

`find`/`get` 동사, 약어, `I` 접두어, 같은 개념의 두 낱말은 리뷰가 체크리스트로 본다.

## 공개 참고

- ddd-by-examples/library [`Patron`](https://github.com/ddd-by-examples/library/blob/master/src/main/java/io/pillopl/library/lending/patron/model/Patron.java) - 업무 용어 하나로 통일한 이름(`placeOnHold`, `BookPlacedOnHold`). 이 예시의 `cancel` 한 낱말과 같은 생각이다.
  - 우리 ADR 과 다른 점: `patronHolds.a(book)` 같은 뜻 없는 이름, `PatronInformation`(Info 류 이름), 중첩 타입(ADR-004).
