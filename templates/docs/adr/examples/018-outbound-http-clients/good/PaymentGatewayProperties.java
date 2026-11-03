@Validated
@ConfigurationProperties("payment-gateway")
public record PaymentGatewayProperties(@NotBlank String baseUrl, @NotNull Timeout timeout) {

    // 설정 키 payment-gateway.timeout.connect, .read 의 깊이를 그대로 따른다(ADR-003, ADR-004 의 허용 예).
    public record Timeout(Duration connect, Duration read) {

        private static final Duration MIN_TIMEOUT = Duration.ofMillis(1);
        // HTTP 클라이언트 구현 가운데 타임아웃을 int 밀리초로 받는 것이 있어, 그 범위를 넘으면 조용히 잘린다.
        private static final Duration MAX_TIMEOUT = Duration.ofMillis(Integer.MAX_VALUE);

        public Timeout {
            requireInRange("payment-gateway.timeout.connect", connect);
            requireInRange("payment-gateway.timeout.read", read);
        }

        private static void requireInRange(String key, Duration value) {
            if (value == null || value.compareTo(MIN_TIMEOUT) < 0 || value.compareTo(MAX_TIMEOUT) > 0) {
                throw new IllegalArgumentException("%s must be between %s and %s: %s"
                    .formatted(key, MIN_TIMEOUT, MAX_TIMEOUT, value));
            }
        }
    }
}
