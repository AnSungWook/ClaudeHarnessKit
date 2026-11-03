@Validated
@ConfigurationProperties(prefix = "payment-gateway")
public record PaymentGatewayProperties(
    @NotBlank String baseUrl,
    @NotBlank String apiKey,
    @NotNull @Valid Timeout timeout) {

    public PaymentGatewayProperties {
        // 환경변수가 없으면 Spring 은 "${PAYMENT_GATEWAY_API_KEY}" 글자를 그대로 넣는다. 그 글자가 키로 쓰이기 전에 기동을 막는다.
        if (apiKey != null && apiKey.startsWith("${")) {
            throw new IllegalArgumentException("payment-gateway.api-key is not resolved");
        }
    }

    @Override
    public String toString() {
        // 기동 때 남기는 설정 요약에 키가 찍히지 않게 가린다.
        return "PaymentGatewayProperties[baseUrl=%s, apiKey=****, timeout=%s]".formatted(baseUrl, timeout);
    }

    public record Timeout(@NotNull Duration connect, @NotNull Duration read) {
        // ... 생성자가 connect, read 가 1ms 이상 int 밀리초(Integer.MAX_VALUE ms) 이하인지 보고, 어기면 그 키 이름을 담아 기동을 막는다
    }
}
