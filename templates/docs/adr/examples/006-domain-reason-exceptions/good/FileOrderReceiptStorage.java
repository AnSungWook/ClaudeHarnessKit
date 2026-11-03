@Component
public class FileOrderReceiptStorage implements OrderReceiptStorage {

    // ...

    @Override
    public void save(OrderReceipt receipt) {
        Path path = receiptPaths.receiptOf(receipt.orderId());
        try {
            Files.writeString(path, receipt.text());
        } catch (IOException e) {
            throw new OrderReceiptStorageUnavailableException(receipt.orderId(), e);
        }
    }
}
