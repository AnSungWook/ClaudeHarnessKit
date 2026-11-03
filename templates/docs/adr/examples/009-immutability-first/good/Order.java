public record Order(long id, long memberId, OrderStatus status, List<OrderLine> lines) {

    public Order {
        // 넘겨준 쪽이 나중에 리스트를 고쳐도 이 주문은 바뀌지 않게 복사해 둔다.
        lines = List.copyOf(lines);
    }

    public Order markPaid() {
        if (isPaid()) {
            throw new OrderAlreadyPaidException(id, status.name());
        }
        return new Order(id, memberId, OrderStatus.PAID, lines);
    }

    public Order withoutProduct(long productId) {
        List<OrderLine> remainingLines = lines.stream()
            .filter(line -> line.productId() != productId)
            .toList();
        return new Order(id, memberId, status, remainingLines);
    }

    // ...
}
