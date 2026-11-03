@EnableResilientMethods
@EnableConfigurationProperties(PaymentGatewayProperties.class)
@Configuration
public class PaymentGatewayClientConfig {

    @Bean
    public RestClient paymentGatewayRestClient(RestClient.Builder builder, PaymentGatewayProperties properties) {
        HttpClientSettings settings = HttpClientSettings.defaults()
            .withTimeouts(properties.timeout().connect(), properties.timeout().read());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }
}
