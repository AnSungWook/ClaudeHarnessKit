public record Order(long id, OrderStatus status, Instant paidAt) {

    // ...

    public Order markPaid(Instant paidAt) {
        if (status == OrderStatus.PAID) {
            // ADR-006 위반: 업무 사유에 범용 예외 - 사유가 문구 속에 숨고, 경계는 409 가 아니라 500 으로 낸다
            throw new IllegalStateException("이미 결제된 주문: " + id);
        }
        return new Order(id, OrderStatus.PAID, paidAt);
    }
}
