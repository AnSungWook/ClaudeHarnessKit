@Service
public class OrderPaymentService {

    // ...

    @Transactional
    public OrderPaymentResult confirmPayment(long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        PaymentApproval approval = paymentApprovals.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentApprovalNotFoundException(orderId));
        return paymentResults.save(OrderPaymentResult.of(order, approval, clock));
    }
}
