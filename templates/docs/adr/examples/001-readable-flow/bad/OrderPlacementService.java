// ADR-001 위반 모음: 서비스만 읽으면 주문 접수가 저장 한 줄로 보이고, 재고 차감·배송 요청·영수증이 어디서 언제 도는지 안 보인다.
@Service
public class OrderPlacementService {

    // ...

    @Transactional
    public PlacedOrder placeOrder(PlaceOrderCommand command) {
        Order order = orders.save(Order.place(command));
        // ADR-001 위반: 응답 전에 끝나야 하는 재고 차감과 반드시 끝나야 하는 배송 요청까지 이벤트 하나에 실어 리스너로 넘긴다
        // ADR-001 위반: 영속 엔티티와 요청 객체를 그대로 싣는다
        events.publishEvent(new OrderChanged(order, command));
        return PlacedOrder.of(order);
    }
}
