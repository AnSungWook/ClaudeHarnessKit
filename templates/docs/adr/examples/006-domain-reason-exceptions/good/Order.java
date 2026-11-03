public record Order(long id, OrderStatus status, Instant paidAt) {

    // ...

    public Order markPaid(Instant paidAt) {
        if (status == OrderStatus.PAID) {
            throw new OrderAlreadyPaidException(id, status);
        }
        return new Order(id, OrderStatus.PAID, paidAt);
    }
}
