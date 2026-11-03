@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public OrderResult getOrder(long orderId) {
        return orders.findById(orderId)
            .map(OrderResult::of)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResult> getOrdersBy(OrderSearchCondition condition) {
        return orders.findAllBy(condition).stream()
            .map(OrderResult::of)
            .toList();
    }
}
