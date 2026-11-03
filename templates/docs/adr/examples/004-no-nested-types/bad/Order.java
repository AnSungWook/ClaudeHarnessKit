public record Order(long id, List<Line> lines) {

    private static final int BULK_QUANTITY = 10;

    // ...

    public boolean hasBulkLine() {
        return lines.stream().anyMatch(Line::isBulk);
    }

    // ADR-004 위반: 주문 줄은 Order 밖(장바구니, 정산)에서도 쓰이는 독립 값인데 중첩해, 그 판단이 Order 파일에 섞이고 Order.Line 으로 불린다
    public record Line(String productCode, int quantity, Money unitPrice) {

        public Money subtotal() {
            return unitPrice.times(quantity);
        }

        public boolean isBulk() {
            return quantity >= BULK_QUANTITY;
        }
    }
}
