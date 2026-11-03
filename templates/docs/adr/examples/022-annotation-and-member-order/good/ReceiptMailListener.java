@RequiredArgsConstructor
@Component
public class ReceiptMailListener {

    private final ReceiptMailService receiptMailService;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendReceiptOnOrderPlaced(OrderPlacedEvent event) {
        receiptMailService.sendReceipt(event.orderId());
    }
}
