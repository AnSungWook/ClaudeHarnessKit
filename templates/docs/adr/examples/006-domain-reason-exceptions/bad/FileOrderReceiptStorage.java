@Component
public class FileOrderReceiptStorage implements OrderReceiptStorage {

    // ...

    @Override
    public void save(OrderReceipt receipt) {
        Path path = receiptPaths.receiptOf(receipt.orderId());
        try {
            Files.writeString(path, receipt.text());
        } catch (IOException e) {
            // ADR-006 위반: 로그만 남기고 삼킨다 - 서비스는 저장된 줄 알고 결제 완료로 넘어간다
            log.warn("Receipt save failed: orderId={}", receipt.orderId(), e);
        }
    }
}
