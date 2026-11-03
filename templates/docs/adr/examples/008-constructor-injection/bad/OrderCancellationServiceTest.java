class OrderCancellationServiceTest {

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();

    @Test
    void cancelOrderMarksOrderCancelledAtClockTime() {
        OrderCancellationService service = new OrderCancellationService();
        // ADR-008 위반: 필드 주입이라 리플렉션으로 넣어야 한다 - 필드 이름을 바꾸면 컴파일은 되고 테스트만 깨진다
        ReflectionTestUtils.setField(service, "orders", orders);
        service.setClock(Clock.fixed(Instant.parse("2026-11-03T01:00:00Z"), ZoneOffset.UTC));
        // ...
    }
}
