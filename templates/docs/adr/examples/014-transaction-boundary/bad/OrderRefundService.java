@RequiredArgsConstructor
@Service
public class OrderRefundService {

    private final OrderRepository orders;
    private final PaymentGateway paymentGateway;

    public OrderRefundResult refundOrder(long orderId) {
        RefundReceipt receipt = paymentGateway.refund(orderId);
        return recordRefund(orderId, receipt); // ADR-014 위반: 같은 클래스 안 호출이라 프록시를 안 거쳐 아래 트랜잭션이 걸리지 않는다
    }

    @Transactional
    public OrderRefundResult recordRefund(long orderId, RefundReceipt receipt) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        Order refundedOrder = orders.save(order.markRefunded(receipt));
        return OrderRefundResult.of(refundedOrder);
    }
}
