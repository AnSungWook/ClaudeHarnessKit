@RequiredArgsConstructor
@Service
public class OrderCancellationService {

    private final OrderRepository orders;

    @Transactional
    public OrderResult cancelOrder(OrderCancelCommand command) {
        Order canceledOrder = orders.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()))
            .cancel();
        orders.save(canceledOrder);
        return OrderResult.of(canceledOrder);
    }
}
