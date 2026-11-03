public enum PaymentDeclineReason {
    INSUFFICIENT_BALANCE,
    CARD_EXPIRED,
    GATEWAY_TIMEOUT;

    // default 를 두지 않아, 사유를 더하면 재시도 여부를 정하기 전에는 컴파일이 안 된다.
    public boolean isRetryable() {
        return switch (this) {
            case INSUFFICIENT_BALANCE, CARD_EXPIRED -> false;
            case GATEWAY_TIMEOUT -> true;
        };
    }
}
