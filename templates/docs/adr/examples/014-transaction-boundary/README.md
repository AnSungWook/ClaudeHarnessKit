# ADR-014 예시: 트랜잭션은 응용 서비스에 두고, 그 안에서 바깥을 부르지 않는다

[ADR-014](../../ADR-014_transaction-boundary.md) 를 주문 결제에 적용한 예다. 결제사 승인(바깥 HTTP)과 영수증 메일(바깥 전송)이 끼는 흔한 흐름이다.

## 무엇을 보여 주나

바깥 호출은 트랜잭션 밖에서 하고, 읽기는 `readOnly` 트랜잭션, 결과 저장은 짧은 쓰기 트랜잭션으로 나눈다.
트랜잭션 안에서는 내부 이벤트 발행까지만 하고, 메일은 커밋 뒤 리스너(`@TransactionalEventListener(phase = AFTER_COMMIT)`)가 보낸다.

```text
orderPaymentService.payOrder(orderId)                         트랜잭션 없음
  orderQueryService.getPayableOrder(orderId)                  readOnly 트랜잭션
  paymentGateway.approve(PaymentApprovalCommand.of(order))    트랜잭션 밖, 결제사 HTTP
  orderPaymentRecordService.recordApproval(orderId, approval) 쓰기 트랜잭션: 저장 + OrderPaidEvent 발행
커밋 뒤 → receiptMailService.sendReceiptOnOrderPaid(event)
```

## 바른 모양

- `good/OrderPaymentService.java` - 흐름만 든다. 트랜잭션을 걸지 않고, 트랜잭션이 필요한 단계는 다른 빈의 메서드를 부른다(프록시를 거치게).
- `good/OrderQueryService.java` - 읽기만 하는 `getPayableOrder` 는 `@Transactional(readOnly = true)`.
- `good/OrderPaymentRecordService.java` - 결과 저장만 하는 짧은 `@Transactional`. 안에서 `publishEvent` 로 커밋 뒤 처리를 건다.
- `good/ReceiptMailService.java` - `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` 로 커밋된 뒤에만 메일을 보낸다. 롤백되면 안 돈다.

## 어긋난 모양

- `bad/OrderPaymentService.java` - `@Transactional` 메서드 안에서 결제사 승인과 메일 전송, 읽기 메서드에 쓰기 트랜잭션.
- `bad/OrderRefundService.java` - 바깥 호출을 트랜잭션 밖으로 빼려고 나눴지만 같은 클래스 안에서 `recordRefund` 를 불러 `@Transactional` 이 걸리지 않는다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@Transactional payOrder` 안의 `paymentGateway.approve(...)` | 트랜잭션 없는 `payOrder` 가 부르고, 저장만 `recordApproval` 트랜잭션 | 트랜잭션 메서드 안에서 HTTP·LLM·외부 메시지 전송·스토리지를 부르지 않는다 |
| 트랜잭션 안의 `receiptMailSender.sendReceipt(...)` | `events.publishEvent(new OrderPaidEvent(...))` + 커밋 뒤 리스너 | 커밋 뒤 해야 할 일이 커밋 전에 실행되지 않는다 |
| 읽기 메서드에 `@Transactional` | `@Transactional(readOnly = true)` | 읽기 전용 서비스 메서드가 `readOnly` 다 |
| `refundOrder` 가 같은 클래스의 `@Transactional recordRefund` 를 부름 | 저장 단계를 다른 빈(`OrderPaymentRecordService`)에 두고 부른다 | 같은 클래스 내부 호출로 트랜잭션을 기대하지 않는다 |

- `@Transactional` 은 서비스 메서드에만 있고 클래스 위에는 없다(ADR-022). 컨트롤러, 리포지토리, 어댑터에는 두지 않는다.
- 나눈 흐름은 중간 실패를 따로 다뤄야 한다(ADR 의 부정적 결과). 승인은 됐는데 저장이 실패하면 결제사에 승인이 남는다. `PaymentApprovalCommand.of(order)` 가 주문 ID 를 멱등 키로 넘겨 같은 주문을 다시 승인해도 한 번만 처리되게 하는 것이 흔한 모양이다 - 이 예시는 이름으로만 둔다.
- 커밋 뒤 리스너는 기본으로 커밋한 스레드에서 돈다. 메일이 느리거나 실패해도 결제 응답을 붙잡지 않게 하려면 `@Async("mailExecutor")` 로 크기를 정한 실행기에 넘긴다(ADR-015).
- `PaymentGateway`, `ReceiptMailSender`(포트), `PaymentApproval`, `OrderPaidEvent`, `OrderPaymentResult`, `Order.validatePayable()`, `Order.markPaid(...)` 는 이름으로만 쓴다.

## 기계 검사

이 ADR 을 막는 기계 검사는 없다. 리뷰가 체크리스트로 본다.

## 공개 참고

- [spring-modulith `OrderManagement`](https://github.com/spring-projects/spring-modulith/blob/main/spring-modulith-examples/spring-modulith-example-full/src/main/java/example/order/OrderManagement.java) - 서비스의 `@Transactional` 안에서는 내부 이벤트 발행만 하고, 후속 일은 커밋 뒤 리스너가 한다.
  - 우리 ADR 과 다른 점: 서비스 빈 이름이 `Service` 로 끝나지 않는다(ADR-016), 클래스 위 어노테이션 순서가 ADR-022 와 다르다.
