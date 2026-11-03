@ConfigurationProperties(prefix = "order")
public record OrderProperties(Payment payment) {

    // 키 깊이(order.payment.timeout.*)를 그대로 따른 묶음이라 중첩한다(ADR-004 의 허용 예).
    public record Payment(URI baseUrl, String apiKey, Timeout timeout) {

        public Payment {
            // ...
            // 환경변수가 없으면 Spring 이 "${...}" 글자를 그대로 넣는다. 그 글자가 키로 쓰이기 전에 기동을 막는다.
            if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
                throw new IllegalArgumentException("order.payment.api-key is not set");
            }
        }

        // ... (toString 은 기동 때 남기는 설정 요약에 키가 새지 않게 apiKey 를 가린다)

        public record Timeout(Duration connect, Duration read) {

            private static final Duration MIN = Duration.ofMillis(1);
            private static final Duration MAX = Duration.ofMillis(Integer.MAX_VALUE);

            public Timeout {
                requireMillisRange("order.payment.timeout.connect", connect);
                requireMillisRange("order.payment.timeout.read", read);
            }

            // 요청 팩토리는 int 밀리초로 적용한다. 1ms 미만은 0(무한 대기)이 되고, int 를 넘으면 넘친다.
            private static void requireMillisRange(String key, Duration value) {
                if (value == null || value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
                    throw new IllegalArgumentException("%s must be 1ms to %dms: %s".formatted(key, Integer.MAX_VALUE, value));
                }
            }
        }
    }
}
