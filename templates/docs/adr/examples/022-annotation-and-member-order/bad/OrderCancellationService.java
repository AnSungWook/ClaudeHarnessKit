@Service // ADR-022 위반: 정체 어노테이션이 맨 위라 선언 바로 위 한 줄로 무엇인지 안 보인다
@Transactional // ADR-022 위반(ADR-014 도): 트랜잭션이 서비스 메서드가 아니라 클래스에 있다
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationService {

    // ...

    public OrderResult cancelOrder(OrderCancelCommand command) {
        Order canceledOrder = getExistingOrder(command.orderId()).cancel();
        publishCanceled(canceledOrder);
        // ...
    }

    public List<OrderResult> cancelOpenOrdersOf(long memberId) {
        List<Order> canceledOrders = findOpenOrders(memberId).stream().map(Order::cancel).toList();
        // ...
    }

    // ADR-022 위반: private 이 파일 끝에 몰려 있어, cancelOrder 가 부르는 단계를 보려면 다른 public 을 건너 내려가야 한다
    private List<Order> findOpenOrders(long memberId) { /* ... */ }

    private void publishCanceled(Order canceledOrder) { /* ... */ }

    private Order getExistingOrder(long orderId) { /* ... */ }
}
