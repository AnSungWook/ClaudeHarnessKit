@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    private final AuthProperties authProperties;

    @Bean
    public InternalTokenAuthenticationFilter internalTokenAuthenticationFilter(ErrorResponseWriter errorResponseWriter) {
        return new InternalTokenAuthenticationFilter(authProperties.internalToken(), errorResponseWriter);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 목록이 비면 매핑을 걸지 않는다. 그러면 교차 출처 요청은 모두 막힌 채다.
        if (authProperties.allowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
            .allowedOrigins(authProperties.allowedOrigins().toArray(String[]::new))
            .allowedMethods("GET", "POST");
    }
}
