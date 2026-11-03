@Validated
@ConfigurationProperties(prefix = "delivery.quote")
public record DeliveryQuoteProperties(@Positive int threads, @Positive int queueCapacity, @NotNull Duration timeout) {

    private static final Duration MIN_TIMEOUT = Duration.ofMillis(1);
    private static final Duration MAX_TIMEOUT = Duration.ofMillis(Integer.MAX_VALUE);

    public DeliveryQuoteProperties {
        // 0 이하는 기다리지 않거나 끝없이 기다리는 뜻이 되고, 너무 큰 값은 밀리초로 바꿀 때 넘친다.
        if (timeout != null && (timeout.compareTo(MIN_TIMEOUT) < 0 || timeout.compareTo(MAX_TIMEOUT) > 0)) {
            throw new IllegalArgumentException(
                "delivery.quote.timeout must be between 1ms and %dms: %s".formatted(Integer.MAX_VALUE, timeout));
        }
    }
}
