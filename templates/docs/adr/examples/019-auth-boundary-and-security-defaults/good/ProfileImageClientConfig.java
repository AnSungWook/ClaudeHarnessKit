@EnableConfigurationProperties(ProfileImageClientProperties.class)
@Configuration
public class ProfileImageClientConfig {

    @Bean
    public RestClient profileImageRestClient(RestClient.Builder builder, ProfileImageClientProperties properties) {
        // 회원이 준 URL 이라 받는 곳을 허용 목록으로 정할 수 없다. 사설, 루프백, 링크 로컬(클라우드 메타데이터)로는 못 가게 한다.
        HttpClientSettings settings = HttpClientSettings.defaults()
            .withTimeouts(properties.timeout().connect(), properties.timeout().read())
            .withInetAddressFilter(InetAddressFilter.externalAddresses());
        return builder.requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(settings)).build();
    }
}
