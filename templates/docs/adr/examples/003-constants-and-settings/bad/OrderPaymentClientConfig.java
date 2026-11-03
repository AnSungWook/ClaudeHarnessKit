@Configuration
public class OrderPaymentClientConfig {

    @Bean
    public RestClient paymentRestClient(RestClient.Builder builder, Environment environment,
        // ADR-003 위반: 설정을 @Value 로 칸마다 받고 기본값이 코드 안에 숨는다(다른 빈이 다른 기본값을 둬도 모른다)
        @Value("${order.payment-base-url:https://pay.example.com}") String baseUrl,
        // ADR-003 위반: 비밀값에 빈 기본값 - 환경변수를 빠뜨려도 빈 키로 뜬다
        @Value("${order.payment-api-key:}") String apiKey,
        @Value("${order.payment-connect-timeout:2s}") Duration connectTimeout,
        @Value("${order.payment-read-timeout:5s}") Duration readTimeout) {
        String targetUrl = baseUrl;
        // ADR-003 위반: 코드가 프로파일 이름으로 갈라 동작한다(환경 파일의 값으로 가른다)
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            targetUrl = "http://localhost:18080";
        }
        // ...
    }
}
