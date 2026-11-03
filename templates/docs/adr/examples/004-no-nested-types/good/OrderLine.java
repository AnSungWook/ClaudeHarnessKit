public record OrderLine(String productCode, int quantity, Money unitPrice) {

    private static final int BULK_QUANTITY = 10;

    // ... (compact 생성자가 수량 1 이상, null 아님을 검사한다)

    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    public boolean isBulk() {
        return quantity >= BULK_QUANTITY;
    }
}
