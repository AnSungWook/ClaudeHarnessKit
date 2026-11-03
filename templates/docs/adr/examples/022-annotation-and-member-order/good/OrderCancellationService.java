@Slf4j
@RequiredArgsConstructor
@Service
public class OrderCancellationService {

    // ...

    @Transactional
    public OrderResult cancelOrder(OrderCancelCommand command) {
        Order canceledOrder = getExistingOrder(command.orderId()).cancel();
        orders.save(canceledOrder);
        publishCanceled(canceledOrder);
        return OrderResult.of(canceledOrder);
    }

    private Order getExistingOrder(long orderId) { /* ... */ }

    private void publishCanceled(Order canceledOrder) { /* ... */ }

    @Transactional
    public List<OrderResult> cancelOpenOrdersOf(long memberId) {
        List<Order> canceledOrders = findOpenOrders(memberId).stream().map(Order::cancel).toList();
        canceledOrders.forEach(this::publishCanceled);
        // ...
    }

    private List<Order> findOpenOrders(long memberId) { /* ... */ }
}
