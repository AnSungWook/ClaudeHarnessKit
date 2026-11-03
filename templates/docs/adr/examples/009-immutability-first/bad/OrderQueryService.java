@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public Order getOrder(long orderId) {
        return orders.findById(orderId).get(); // ADR-009 위반: Optional.get() - 없으면 NoSuchElementException 이라 사유가 사라진다
    }

    @Transactional(readOnly = true)
    public List<Order> getUnpaidOrders(long memberId) {
        List<Order> memberOrders = orders.findAllByMemberId(memberId);
        if (memberOrders.isEmpty()) {
            return null; // ADR-009 위반: 컬렉션 대신 null - 부르는 쪽마다 null 검사가 퍼진다
        }
        memberOrders.removeIf(order -> order.getStatus() == OrderStatus.PAID); // ADR-009 위반: 받은 컬렉션을 고친다(불변 리스트면 UnsupportedOperationException)
        return memberOrders;
    }
}
