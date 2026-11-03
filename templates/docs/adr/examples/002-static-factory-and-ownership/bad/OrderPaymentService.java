@Service
public class OrderPaymentService {

    // ...

    @Transactional
    public OrderPaymentResult confirmPayment(long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        PaymentApproval approval = paymentApprovals.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentApprovalNotFoundException(orderId));
        OrderPaymentResult result = OrderPaymentResult.of(order.id(),
            // ADR-002 위반: 상태를 서비스가 생성 인자 안의 삼항으로 정한다
            approval.isApproved() ? OrderPaymentStatus.PAID : OrderPaymentStatus.PENDING,
            // ADR-002 위반: 대기 사유를 서비스가 조건과 문자열 + 로 조립한다
            approval.isApproved() ? null : "결제 대기: " + approval.declineReason(),
            clock.instant());
        return paymentResults.save(result);
    }
}
