@Service
public class OrderQueryService {

    private final OrderRepository orders;

    // ...

    @Transactional(readOnly = true)
    public Order getOrder(long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
