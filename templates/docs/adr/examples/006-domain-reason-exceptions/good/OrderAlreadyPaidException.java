public class OrderAlreadyPaidException extends ConflictException {

    private static final String MESSAGE = "Order already paid: orderId=%d, status=%s";

    public OrderAlreadyPaidException(long orderId, OrderStatus status) {
        super(OrderErrorCode.ORDER_ALREADY_PAID, MESSAGE.formatted(orderId, status));
    }
}
