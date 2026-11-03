class OrderTest {

    private static final Instant PLACED_AT = Instant.parse("2026-11-03T01:00:00Z");

    @Test
    void totalAmountAddsUpEveryLine() {
        Order order = Order.place(7L, List.of(new OrderLine(10L, 2, 1_500), new OrderLine(11L, 1, 3_000)), PLACED_AT);

        assertThat(order.totalAmount()).isEqualTo(6_000);
    }

    @Test
    void markPaidRejectsAlreadyPaidOrder() {
        Order paidOrder = Order.place(7L, List.of(new OrderLine(10L, 1, 1_000)), PLACED_AT).markPaid();

        assertThatThrownBy(paidOrder::markPaid).isInstanceOf(OrderAlreadyPaidException.class);
    }

    // ...
}
