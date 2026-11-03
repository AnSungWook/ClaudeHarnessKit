@RequiredArgsConstructor
@Service
public class OrderPaymentService {

    private final OrderQueryService orderQueryService;
    private final PaymentGateway paymentGateway;
    private final OrderPaymentRecordService orderPaymentRecordService;

    // 이 메서드에는 트랜잭션을 걸지 않는다. 결제사 응답을 기다리는 동안 DB 연결과 잠금을 붙잡지 않게, 읽기와 저장만 각자 짧게 건다.
    public OrderPaymentResult payOrder(long orderId) {
        Order order = orderQueryService.getPayableOrder(orderId);
        PaymentApproval approval = paymentGateway.approve(PaymentApprovalCommand.of(order));
        return orderPaymentRecordService.recordApproval(orderId, approval);
    }
}
