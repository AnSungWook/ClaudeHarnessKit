public record OrderPaymentResult(long orderId, OrderPaymentStatus status, String pendingReason, Instant decidedAt) {

    public OrderPaymentResult {
        // ...
        if (status == OrderPaymentStatus.PAID && pendingReason != null) {
            throw new IllegalArgumentException("paid result has a pending reason: orderId=%d".formatted(orderId));
        }
        if (status == OrderPaymentStatus.PENDING && (pendingReason == null || pendingReason.isBlank())) {
            throw new IllegalArgumentException("pending result has no reason: orderId=%d".formatted(orderId));
        }
    }

    public static OrderPaymentResult of(Order order, PaymentApproval approval, Clock clock) {
        Instant decidedAt = clock.instant();
        if (approval.isApproved()) {
            return new OrderPaymentResult(order.id(), OrderPaymentStatus.PAID, null, decidedAt);
        }
        String pendingReason = "결제 대기: %s".formatted(approval.declineReason());
        return new OrderPaymentResult(order.id(), OrderPaymentStatus.PENDING, pendingReason, decidedAt);
    }
}
