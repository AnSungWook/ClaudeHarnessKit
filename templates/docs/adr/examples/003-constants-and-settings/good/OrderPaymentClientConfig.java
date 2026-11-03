@EnableConfigurationProperties(OrderProperties.class)
@Configuration
public class OrderPaymentClientConfig {

    // ...

    @Bean
    public RestClient paymentRestClient(RestClient.Builder builder, OrderProperties properties) {
        OrderProperties.Payment payment = properties.payment();
        log.info("Order payment client settings: {}", payment);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(payment.timeout().connect());
        requestFactory.setReadTimeout(payment.timeout().read());
        // ...
    }
}
