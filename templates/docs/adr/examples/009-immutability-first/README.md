# ADR-009 예시: 불변을 먼저 쓴다

[ADR-009](../../ADR-009_immutability-first.md) 를 주문 값과 주문 조회에 적용한 예다.

## 무엇을 보여 주나

주문(`Order`)과 회원의 주문 묶음(`MemberOrders`)을 record 로 두고, 상태를 바꾸거나 거를 때는 새 값을 돌려준다.
저장소 포트는 없을 수 있는 하나를 `Optional`, 여럿을 빈 리스트로 돌려주고, 서비스는 `orElseThrow(사유 예외)` 로 받는다.

## 바른 모양

- `good/Order.java` - 받은 리스트를 `List.copyOf` 로 방어 복사하고, `markPaid()`, `withoutProduct()` 가 새 `Order` 를 돌려준다.
- `good/MemberOrders.java` - 회원의 주문 묶음. 받은 리스트를 복사해 들고, "미결제 주문 고르기" 판단을 여기서 하며 걸러낸 새 리스트(`toList()`)를 돌려준다(ADR-002, 판단은 서비스가 아니라 값 타입에).
- `good/OrderRepository.java` - 포트 읽기는 `Optional` 과 빈 리스트. `Optional` 은 반환값에만 있다.
- `good/OrderQueryService.java` - `orElseThrow(() -> new OrderNotFoundException(...))`. 미결제 주문은 `new MemberOrders(...).unpaidOrders()` 를 부르기만 한다.

## 어긋난 모양

- `bad/Order.java` - 가변 클래스. public setter, `Optional` 필드와 매개변수, 받은 리스트를 그대로 들고 그대로 내보낸다.
- `bad/OrderQueryService.java` - `Optional.get()`, 빈 결과를 `null` 로, 포트가 준 리스트를 `removeIf` 로 고친다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `order.setStatus(PAID)` | `Order paidOrder = order.markPaid()` | public setter 가 없다(엔티티 포함, 상태 변경은 이름 있는 메서드) |
| `this.lines = lines;`, `return lines;` | 생성자에서 `lines = List.copyOf(lines)` | 새 값·전달 타입이 record 이거나 모든 필드가 `final` 이다 / 돌려주는 컬렉션이 불변이다 |
| `private Optional<Instant> paidAt`, `setPaidAt(Optional<Instant>)` | `Optional` 은 `findById` 반환값에만 | `Optional` 이 필드·매개변수에 없다 |
| `memberOrders.removeIf(...)` | `MemberOrders.unpaidOrders()` 가 `filter(...).toList()` 로 새 리스트 | 매개변수로 받은 컬렉션을 직접 고치지 않는다 |
| `return null;` | 포트와 서비스 모두 빈 리스트 | 컬렉션을 돌려주는 메서드가 `null` 을 돌려주지 않는다 |
| `orders.findById(orderId).get()` | `orElseThrow(() -> new OrderNotFoundException(orderId))` | `Optional.get()` 이 없다 |

- JPA 엔티티는 이 ADR 의 예외(가변)다. 그때도 public setter 없이 `markPaid(paidAt)` 같은 이름 있는 메서드로만 바꾼다 - 아래 공개 참고가 그 모양이다. 이 예시는 엔티티를 도메인과 나눠 둔 헥사고날 배치(ADR-005)라 도메인 쪽만 보인다.
- `OrderLine`(값 record), `OrderStatus`, `OrderAlreadyPaidException`, `OrderNotFoundException` 은 이름으로만 쓴다(예외는 공통 형제 상속, ADR-006).

## 기계 검사

- [`checks/AdrArchitectureTest.java`](../../checks/README.md) 의 `Optional` 필드 규칙이 필드의 `Optional` 을 막는다.
- setter, `Optional` 매개변수, `Optional.get()`, 받은 컬렉션 고치기, `null` 반환은 리뷰가 체크리스트로 본다.

## 공개 참고

- [odrotbohm/spring-restbucks `Order`](https://github.com/odrotbohm/spring-restbucks/blob/main/server/src/main/java/de/odrotbohm/restbucks/order/Order.java) - setter 없는 엔티티, 이름 있는 상태 변경(`markPaid`).
  - 우리 ADR 과 다른 점: `@Getter` 가 가변 `lineItems` 를 그대로 내보낸다(우리는 불변 복사본), 업무 사유에 `IllegalStateException` 을 쓴다(우리는 사유 예외, ADR-006), `LocalDateTime.now()` 를 부른다(우리는 주입한 `Clock`, ADR-003).
