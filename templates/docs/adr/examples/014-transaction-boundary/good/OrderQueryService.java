@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public Order getPayableOrder(long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.validatePayable();
        return order;
    }
}
