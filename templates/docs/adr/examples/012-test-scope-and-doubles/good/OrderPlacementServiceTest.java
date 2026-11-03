class OrderPlacementServiceTest {

    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final OrderPlacementService orderPlacementService =
        new OrderPlacementService(orders, Clock.fixed(Instant.parse("2026-11-03T01:00:00Z"), ZoneOffset.UTC));

    @Test
    void placeOrderReturnsTotalAmountOfLines() {
        OrderPlacementResult result = orderPlacementService.placeOrder(
            new PlaceOrderCommand(7L, List.of(new OrderLine(10L, 2, 1_500))));

        assertThat(result.totalAmount()).isEqualTo(3_000);
    }

    @Test
    void placeOrderSavesOrderPlacedAtClockTime() {
        orderPlacementService.placeOrder(new PlaceOrderCommand(7L, List.of(new OrderLine(10L, 2, 1_500))));

        assertThat(orders.savedOrders())
            .singleElement()
            .satisfies(order -> assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-11-03T01:00:00Z")));
    }
}
