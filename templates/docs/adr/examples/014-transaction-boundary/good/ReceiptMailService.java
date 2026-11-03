@RequiredArgsConstructor
@Service
public class ReceiptMailService {

    private final ReceiptMailSender receiptMailSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendReceiptOnOrderPaid(OrderPaidEvent event) {
        receiptMailSender.sendReceipt(event.orderId());
    }
}
