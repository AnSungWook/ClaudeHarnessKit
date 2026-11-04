// order/application/service 에 있는 리스너 묶음 - 자리부터 ADR-017 위반(입구는 adapter/in/event).
@Component
public class OrderChangedListeners {

    @Order(1)   // ADR-001 위반: 재고 차감이 배송 요청보다 먼저여야 한다는 업무 순서를 @Order 에 숨긴다
    @EventListener   // 동기 리스너 - 여기서 난 예외가 주문 접수 요청을 실패시킨다. 그런데 서비스에는 안 보인다
    public void onEvent(OrderChanged event) {
        stockJpaRepository.decrease(event.order().items());   // ADR-001 위반: 리스너가 out 어댑터를 직접 부른다
    }

    @Order(2)
    @EventListener
    public void onEvent2(OrderChanged event) {
        try {
            shippingClient.request(event.order());   // ADR-014 위반: 트랜잭션 안에서 바깥 호출
            mailClient.send(event.command().email(), "receipt");
        } catch (Exception e) {
            // ADR-001 위반: 실패를 삼킨다 - 배송 요청이 사라져도 아무도 모른다
        }
    }
}
