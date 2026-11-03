@Validated
@ConfigurationProperties("auth")
public record AuthProperties(@NotBlank String internalToken, List<String> allowedOrigins) {

    private static final String UNRESOLVED_PLACEHOLDER = "${";
    private static final String ANY_ORIGIN = "*";

    public AuthProperties {
        // 환경변수가 없으면 Spring 이 ${INTERNAL_TOKEN} 글자를 그대로 넣는다. 그 글자가 토큰으로 쓰이지 않게 기동을 막는다.
        if (internalToken != null && internalToken.startsWith(UNRESOLVED_PLACEHOLDER)) {
            throw new IllegalArgumentException("auth.internal-token is not resolved");
        }
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        if (allowedOrigins.contains(ANY_ORIGIN)) {
            throw new IllegalArgumentException("auth.allowed-origins must list origins explicitly");
        }
    }
}
