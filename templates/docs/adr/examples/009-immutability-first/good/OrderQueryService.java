@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public Order getOrder(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<Order> getUnpaidOrders(long memberId) {
        return new MemberOrders(orders.findAllByMemberId(memberId)).unpaidOrders();
    }
}
