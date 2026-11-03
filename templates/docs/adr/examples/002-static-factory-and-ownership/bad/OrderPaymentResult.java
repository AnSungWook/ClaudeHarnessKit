// ADR-002 위반: 불변 조건 검사가 없어 PAID 인데 대기 사유가 든 값, 사유 없는 PENDING 이 만들어진다
public record OrderPaymentResult(long orderId, OrderPaymentStatus status, String pendingReason, Instant decidedAt) {

    // ADR-002 위반: 본문이 생성자 복사본뿐인 이름뿐인 팩토리 - 판단이 없으면 생성자를 쓴다
    public static OrderPaymentResult of(long orderId, OrderPaymentStatus status, String pendingReason,
        Instant decidedAt) {
        return new OrderPaymentResult(orderId, status, pendingReason, decidedAt);
    }
}
