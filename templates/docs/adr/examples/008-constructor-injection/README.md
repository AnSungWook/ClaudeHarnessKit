# ADR-008 예시: 의존은 생성자로 받고 final 로 둔다

## 무엇을 보여 주나

주문 취소 서비스와 그 단위 테스트다. 바른 모양은 저장소 포트와 `Clock` 을 생성자로 받아 `final` 로 두고, 테스트가 컨테이너 없이 `new` 로 만든다.
어긋난 모양은 필드 주입과 setter 주입이라, 테스트가 리플렉션과 setter 호출로 의존을 채워 넣어야 한다.
시각을 주입한 `Clock` 에서 얻고 테스트에서 고정 시계를 넣는 모양(ADR-003, ADR-012)도 이 예시가 보인다.

## 바른 모양

- `good/OrderCancellationService.java` - 생성자 하나, 필드 둘 다 `final`. 생성자가 하나라 `@Autowired` 도 없다
- `good/OrderCancellationServiceTest.java` - `new OrderCancellationService(orders, Clock.fixed(...))`. 바깥 의존(저장소 포트)만 가짜 구현(`InMemoryOrderRepository`)이고, 결과 값을 리터럴로 단언한다

`Order.placed(...)`, `order.cancel(...)`, `InMemoryOrderRepository` 는 이름만 나온다.

선택적 의존은 `null` 필드나 `Optional` 생성자 인자 대신 `ObjectProvider` 로 받는다.

```java
public OrderReceiptMailListener(ObjectProvider<ReceiptMailSender> receiptMailSender) {
    this.receiptMailSender = receiptMailSender;
}
```

## 어긋난 모양

- `bad/OrderCancellationService.java` - `@Autowired` 필드 주입, `@Autowired` setter 주입. 필드가 `final` 이 아니다
- `bad/OrderCancellationServiceTest.java` - 같은 테스트를 하려고 `ReflectionTestUtils.setField(service, "orders", ...)` 와 `setClock(...)` 을 부른다. 하나를 빠뜨려도 컴파일은 통과하고 실행에서 `NullPointerException` 이다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `@Autowired private OrderRepository orders;` | `private final OrderRepository orders;` + 생성자 | 필드에 `@Autowired`·`@Inject` 가 없다 |
| `private Clock clock;` 를 setter 로 채움 | `private final Clock clock;` 을 생성자로 | 주입받는 필드가 모두 `final` 이다 / setter 주입이 없다 |
| 테스트가 `ReflectionTestUtils.setField` | 테스트가 `new OrderCancellationService(orders, fixedClock)` | (ADR-008 의도: 테스트에서 프레임워크 없이 객체를 만든다) |

생성자 인자가 계속 늘어 클래스가 하는 일이 한 이름으로 말해지지 않으면 주입 방식이 아니라 클래스를 나눈다. 순환 의존은 `@Lazy` 나 setter 가 아니라 포트 분리나 이벤트로 끊는다.

## 기계 검사

- `checks/AdrArchitectureTest.java` 의 `noFieldInjection` 이 필드 주입을 막는다(`bad/OrderCancellationService.java` 의 `orders` 가 걸린다).
- setter 주입, `final` 누락, `Optional` 생성자 인자는 이 규칙이 안 본다 - 리뷰가 체크리스트로 본다.

## 공개 참고

- spring-petclinic [`OwnerController`](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java) - 생성자 하나에 `final` 필드, `@Autowired` 없음. 다른 점: 컨트롤러가 저장소를 바로 부른다(ADR-005 는 서비스와 포트를 거친다), `processCreationForm` 같은 `process` 이름을 쓴다(ADR-001 은 맡은 일을 말하는 이름).
