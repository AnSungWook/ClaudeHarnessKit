@RequiredArgsConstructor
@Service
public class MemberOrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public OrderResult getMemberOrder(long memberId, long orderId) {
        Order order = orders.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.validateOwnedBy(memberId); // ADR-019 위반: 남의 주문이면 OrderAccessForbiddenException(403)이라 그 주문이 있다는 것을 알려 준다
        return OrderResult.of(order);
    }
}
