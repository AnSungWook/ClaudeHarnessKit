@Component // ADR-022 위반: 정체 어노테이션 다음 줄에 다른 어노테이션이 온다
@RequiredArgsConstructor
public class ReceiptMailListener {

    private final ReceiptMailService receiptMailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ADR-022 위반: 정체(이벤트 수신)가 @Async 위에 있다
    @Async("mailExecutor")
    public void sendReceiptOnOrderPlaced(OrderPlacedEvent event) {
        receiptMailService.sendReceipt(event.orderId());
    }
}
