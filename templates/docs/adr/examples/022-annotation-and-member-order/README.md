# ADR-022 예시: 정체 어노테이션은 선언 바로 위, 멤버는 public 밑에 그것이 부르는 private

[ADR-022](../../ADR-022_annotation-and-member-order.md) 를 주문 취소 서비스, 주문 도메인, 이벤트 리스너, 요청 record 에 적용한 예다.

## 무엇을 보여 주나

선언 바로 위 한 줄(`@Service`, `@Component`, `@TransactionalEventListener`)만 보고 그것이 무엇인지 알 수 있게 어노테이션을 놓는다.
위에서 아래로 한 번 읽으면 public 하나와 그것이 부르는 private 이 붙어서 읽히게 멤버를 놓는다.

**이 킷의 어노테이션 순서는 Spring 공식 샘플과 흔한 공개 예제와 반대다.** 공개 코드는 대개 `@Service` 나 `@RestController` 를 맨 위에 두고 Lombok 을 그 아래에 둔다. 이 킷은 정체 어노테이션을 선언 바로 위(맨 아래)에 둔다. 공개 코드를 베낄 때 순서를 이 킷대로 바꾼다.

## 바른 모양

- [`good/OrderCancellationService.java`](good/OrderCancellationService.java) - `@Slf4j`, `@RequiredArgsConstructor`, `@Service` 순. `@Transactional` 은 메서드에. `cancelOrder` 밑에 그것이 부르는 `getExistingOrder`, `publishCanceled` 를 부르는 순서대로, `cancelOpenOrdersOf` 밑에 `findOpenOrders`. 둘이 같이 쓰는 `publishCanceled` 는 처음 부르는 `cancelOrder` 아래에 있다.
- [`good/Order.java`](good/Order.java) - 도메인 엔티티의 자리 순서: 필드, 정적 팩토리(`place`), 상태 변경(`cancel`), 판단(`isCancellable`), 계산된 값(`totalAmount`), 도메인 값 변환(`toOrderSummary`). 각 public 밑에 그것이 부르는 private(검증 메서드, private 생성자)을 부르는 순서대로 둔다. 도메인은 바깥 라이브러리를 import 하지 않으므로(ADR-005) Lombok 없이 필요한 접근자(`getId()`)만 직접 둔다.
- [`good/ReceiptMailListener.java`](good/ReceiptMailListener.java) - 멤버도 같은 원칙: `@Async` 다음 줄에 `@TransactionalEventListener`.
- [`good/OrderCancelRequest.java`](good/OrderCancelRequest.java) - 요청 record 칸은 검증 어노테이션 다음 줄에 `@JsonProperty`. 길이 상한은 이름 붙은 상수 `MAX_MEMO_LENGTH` 다(ADR-016).

## 어긋난 모양

- [`bad/OrderCancellationService.java`](bad/OrderCancellationService.java) - `@Service` 가 맨 위, `@Transactional` 이 클래스에, private 셋이 파일 끝에 몰려 있다.
- [`bad/ReceiptMailListener.java`](bad/ReceiptMailListener.java) - `@Component` 다음 줄에 `@RequiredArgsConstructor`, `@TransactionalEventListener` 다음 줄에 `@Async`.
- [`bad/Order.java`](bad/Order.java) - 엔티티 자리 순서가 뒤집혀 있고, `cancel` 이 부르는 검증이 파일 끝에 있고, 응답으로 바꾸는 `toResponse()` 가 있다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@Service`, `@Transactional`, `@RequiredArgsConstructor`, `@Slf4j` 순 | `@Slf4j`, `@RequiredArgsConstructor`, `@Service` 순 | 정체 어노테이션 다음 줄에 다른 어노테이션이 없다 |
| 클래스의 `@Transactional` | 각 public 서비스 메서드의 `@Transactional` | `@Transactional` 이 클래스가 아니라 서비스 메서드에 있다 |
| `@TransactionalEventListener` 다음 줄에 `@Async` | `@Async` 다음 줄에 `@TransactionalEventListener` | 정체 어노테이션 다음 줄에 다른 어노테이션이 없다 |
| public 둘 뒤에 private 셋을 몰아 둠 | public 하나 밑에 그것이 부르는 private 을 부르는 순서대로 | private 메서드가 그것을 처음 부르는 public 아래에 있다 |
| 엔티티가 계산, 판단, 상태 변경 순이고 검증이 파일 끝 | 정적 팩토리, 상태 변경, 판단, 계산, 변환 순이고 검증이 `cancel` 바로 아래 | 엔티티도 private 이 그것을 부르는 public 아래에 있고, 자리 순서는 public 멤버끼리 맞는다 |
| `toResponse()` | 응답은 `OrderResponse.of(...)`, 엔티티는 `toOrderSummary()` 만 | 엔티티의 `to...` 가 도메인 값 변환뿐이다 |

- 엔티티 자리 가운데 "연관 할당" 은 이 예시의 `Order` 에 없어 빠졌다. 있으면 정적 팩토리와 상태 변경 사이에 둔다.
- 테스트 어노테이션은 한 저장소 안에서 한 모양이면 된다. 새 프로젝트면 정체 원칙대로 `@DisplayName` 다음 줄에 `@Test` 다.

```java
@DisplayName("결제 전 주문은 취소된다")
@Test
void cancelsOrderBeforePayment() {
```

## 기계 검사

리뷰가 체크리스트로 본다. [`checks/`](../../checks/README.md) 견본에는 이 ADR 을 막는 규칙이 없다. 메서드 자리는 포매터 정렬로도 못 맞추니(ADR-007) 사람이 지킨다.

공개 참고는 두지 않는다. Spring 공식 샘플과 흔한 공개 예제가 어노테이션 순서를 이 킷과 반대로 써서, 참고로 걸면 오히려 어긋난 모양을 퍼뜨린다.
