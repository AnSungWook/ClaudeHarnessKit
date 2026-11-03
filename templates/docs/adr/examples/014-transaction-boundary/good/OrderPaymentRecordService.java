@RequiredArgsConstructor
@Service
public class OrderPaymentRecordService {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    @Transactional
    public OrderPaymentResult recordApproval(long orderId, PaymentApproval approval) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        Order paidOrder = orders.save(order.markPaid(approval));
        events.publishEvent(new OrderPaidEvent(paidOrder.id()));
        return OrderPaymentResult.of(paidOrder);
    }
}
