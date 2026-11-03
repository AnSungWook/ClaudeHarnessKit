@Slf4j
@RequiredArgsConstructor
@Service
public class OrderPlacementService {

    private final OrderRepository orders;
    private final Clock clock;

    @Transactional
    public OrderPlacementResult placeOrder(PlaceOrderCommand command) {
        Order savedOrder = orders.save(Order.place(command.memberId(), command.lines(), clock.instant()));
        log.info("Order placed: orderId={}, memberId={}, totalAmount={}",
            savedOrder.id(), savedOrder.memberId(), savedOrder.totalAmount());
        return OrderPlacementResult.of(savedOrder);
    }
}
