public record Payment(long id, long orderId, long amount, PaymentStatus status) {

    public Payment completeWith(PaymentOutcome outcome) {
        if (outcome instanceof PaymentOutcome.Declined) {
            PaymentOutcome.Declined declined = (PaymentOutcome.Declined) outcome; // ADR-021 위반: instanceof 뒤에 따로 캐스팅한다
            switch (declined.reason()) { // ADR-021 위반: 값 하나를 고르는데 문장형 switch 를 쓰고, enum 판단이 enum 밖에 있다
                case GATEWAY_TIMEOUT:
                    return withStatus(PaymentStatus.RETRY_WAITING);
                default: // ADR-021 위반: enum switch 의 default 가 새 사유를 조용히 거절로 삼킨다
                    return withStatus(PaymentStatus.DECLINED);
            }
        }
        return withStatus(PaymentStatus.APPROVED); // ADR-021 위반: sealed 갈래를 다 나열하지 않아, 새 갈래가 생기면 컴파일러 대신 승인으로 빠진다
    }

    public Payment withStatus(PaymentStatus newStatus) {
        return new Payment(id, orderId, amount, newStatus);
    }
}
