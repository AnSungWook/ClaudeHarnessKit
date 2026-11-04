// order/adapter/in/event - 같은 기능의 커밋 뒤 실행. 리스너는 유스케이스에 넘기기만 한다(ADR-001, ADR-014).
@Component
public class ReceiptOnOrderPlacedListener {

    private final SendReceiptUseCase sendReceipt;   // order 의 port/in. 이 기능은 입구가 둘(웹, 이벤트)이라 port/in 을 거친다(ADR-017)

    // ...

    // 언제 도는지가 선언에서 보인다: 커밋이 된 뒤, 정해진 실행기에서(ADR-015).
    @Async("receiptExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendReceiptOnOrderPlaced(OrderPlacedEvent event) {
        sendReceipt.sendFor(event.orderId());
    }
}

// SendReceiptUseCase 구현(order/application/service)이 메일 out 포트를 트랜잭션 밖에서 부르고,
// 실패하면 ReceiptSendFailures 에 기록해 재발송 배치가 다시 보낸다. 리스너에서 예외를 삼키지 않는다.
