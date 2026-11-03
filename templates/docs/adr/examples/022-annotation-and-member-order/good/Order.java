public final class Order {

    private final Long id;
    // ... memberId, lines, status

    public static Order place(long memberId, List<OrderLine> lines) {
        validateHasLines(lines);
        return new Order(null, memberId, List.copyOf(lines), OrderStatus.PLACED);
    }

    private static void validateHasLines(List<OrderLine> lines) { /* ... */ }

    private Order(Long id, long memberId, List<OrderLine> lines, OrderStatus status) { /* ... */ }

    public Order cancel() {
        validateCancellable();
        return new Order(id, memberId, lines, OrderStatus.CANCELED);
    }

    private void validateCancellable() { /* ... */ }

    public boolean isCancellable() { /* ... */ }

    public Long getId() { /* ... */ }

    public long totalAmount() { /* ... */ }

    public OrderSummary toOrderSummary() { /* ... */ }
}
