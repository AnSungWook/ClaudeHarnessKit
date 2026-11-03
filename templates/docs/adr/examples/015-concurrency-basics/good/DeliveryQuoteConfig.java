@Configuration
public class DeliveryQuoteConfig {

    @Bean
    public ThreadPoolTaskExecutor quoteExecutor(DeliveryQuoteProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.threads());
        executor.setMaxPoolSize(properties.threads());
        executor.setQueueCapacity(properties.queueCapacity());
        // 큐까지 차거나 실행기가 닫혔으면 거부 예외를 던진다. 조용히 버리면 그 작업의 Future 가 끝나지 않는다.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadNamePrefix("delivery-quote-");
        return executor;
    }

    @Bean
    public DeliveryQuotePolicy deliveryQuotePolicy(DeliveryQuoteProperties properties) {
        // 서비스가 설정 층을 모르게, 검증된 값을 응용 층의 타입으로 바꿔 넘긴다.
        return new DeliveryQuotePolicy(properties.timeout());
    }
}
