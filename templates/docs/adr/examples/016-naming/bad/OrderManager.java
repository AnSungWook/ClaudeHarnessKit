@RequiredArgsConstructor
@Service
public class OrderManager { // ADR-016 위반: @Service 가 Service 로 끝나지 않고, Manager 는 맡은 일을 말하지 않는다

    private final IOrderRepository ordRepo; // ADR-016 위반: 새로 만든 약어(ord)

    public OrderDto findOrder(long id) { // ADR-016 위반: 서비스 읽기가 find 라 없을 때 예외인지 빈 값인지 이름이 말하지 않는다
        Order data = ordRepo.getOrder(id) // ADR-016 위반: data 는 담은 것을 말하지 않는다
            .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderDto.of(data); // ADR-016 위반: Dto 하나를 요청, 응답, 서비스 반환에 같이 쓴다
    }

    public List<OrderDto> selectOrderList(OrderDto param) { // ADR-016 위반: 서비스 읽기가 get 이 아니고, 조건을 받는데 By 가 없다
        List<Order> orderList = ordRepo.selectOrderList(param); // ADR-016 위반: 이름에 타입(List)을 넣는다
        // ...
    }

    public OrderDto revokeOrder(long id) { // ADR-016 위반: 도메인은 cancel 인데 여기서는 같은 일을 revoke 로 부른다
        // ...
    }
}
