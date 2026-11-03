public record Order(long id, long memberId, OrderStatus status, long paidAmount, boolean hasCoupon) {

    public Order cancel() {
        if (!status.isCancellable()) {
            throw new OrderNotCancellableException(id, status);
        }
        return new Order(id, memberId, OrderStatus.CANCELED, paidAmount, hasCoupon);
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID;
    }
}
