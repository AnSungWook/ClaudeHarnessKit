class OrderCancellationServiceTest {

    private static final Instant CANCELLED_AT = Instant.parse("2026-11-03T01:00:00Z");

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final OrderCancellationService service =
        new OrderCancellationService(orders, Clock.fixed(CANCELLED_AT, ZoneOffset.UTC));

    @Test
    void cancelOrderMarksOrderCancelledAtClockTime() {
        orders.save(Order.placed(1L, Instant.parse("2026-11-02T09:00:00Z")));

        Order cancelled = service.cancelOrder(1L);

        assertEquals(Instant.parse("2026-11-03T01:00:00Z"), cancelled.cancelledAt());
    }
}
