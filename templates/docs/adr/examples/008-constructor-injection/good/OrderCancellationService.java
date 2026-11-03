@Service
public class OrderCancellationService {

    private final OrderRepository orders;
    private final Clock clock;

    public OrderCancellationService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional
    public Order cancelOrder(long orderId) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        return orders.save(order.cancel(clock.instant()));
    }
}
