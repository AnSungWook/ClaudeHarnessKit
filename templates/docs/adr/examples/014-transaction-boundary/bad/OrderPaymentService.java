@RequiredArgsConstructor
@Service
public class OrderPaymentService {

    private final OrderRepository orders;
    private final PaymentGateway paymentGateway;
    private final ReceiptMailSender receiptMailSender;

    @Transactional
    public OrderPaymentResult payOrder(long orderId) {
        Order order = getPayableOrder(orderId);
        PaymentApproval approval = paymentGateway.approve(PaymentApprovalCommand.of(order)); // ADR-014 위반: 트랜잭션 안에서 결제사 호출 - 기다리는 동안 연결을 붙잡고, 롤백돼도 승인은 안 돌아온다
        Order paidOrder = orders.save(order.markPaid(approval));
        receiptMailSender.sendReceipt(paidOrder.id()); // ADR-014 위반: 커밋 전에 메일 - 커밋이 실패해도 영수증은 이미 나갔다
        return OrderPaymentResult.of(paidOrder);
    }

    @Transactional // ADR-014 위반: 읽기만 하는데 쓰기 트랜잭션
    public Order getPayableOrder(long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.validatePayable();
        return order;
    }
}
