// order/application/service - 주문 접수. 응답 전에 끝나야 하는 일, 나중에라도 끝나야 하는 일, 실패해도 되는 후속 일을 가른다(ADR-001).
@Service
public class OrderPlacementService implements PlaceOrderUseCase {

    private final ReserveStockUseCase reserveStock;          // 다른 기능(inventory)의 port/in
    private final OrderRepository orders;                    // out 포트
    private final ShippingRequestOutbox shippingRequests;    // out 포트 - 배송 요청 책임을 남기는 아웃박스
    private final ApplicationEventPublisher events;

    // ...

    @Override
    @Transactional
    public PlacedOrder placeOrder(PlaceOrderCommand command) {
        // 응답 전에 끝나야 한다 - 재고가 없으면 주문을 받으면 안 된다. 명시적 호출.
        StockReservation reservation = reserveStock.reserveFor(command.items());

        Order order = orders.save(Order.place(command, reservation));

        // 나중에라도 반드시 끝나야 한다 - 배송 시스템 호출은 트랜잭션 밖이 맞으니, 책임만 같은 트랜잭션에 남긴다.
        // 전달·재시도·중복 처리는 아웃박스 전달기가 든다(프로젝트 ADR).
        shippingRequests.recordFor(order.orderId());

        // 실패해도 주문 결과를 바꾸지 않는 후속 일(영수증, 포인트) - 사실만 알린다. 키만 싣는다.
        events.publishEvent(new OrderPlacedEvent(order.orderId(), order.memberKey()));

        return PlacedOrder.of(order);
    }
}
