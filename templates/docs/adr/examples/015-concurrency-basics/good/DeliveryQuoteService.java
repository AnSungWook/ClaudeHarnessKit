@Service
public class DeliveryQuoteService {

    private final List<CarrierQuoteSource> carriers;
    private final Executor quoteExecutor;
    private final DeliveryQuotePolicy policy;
    // ... 생성자로 받는다. quoteExecutor 는 @Qualifier("quoteExecutor")

    public List<DeliveryQuote> getQuotes(DeliveryAddress address) {
        try {
            List<CompletableFuture<DeliveryQuote>> pendingQuotes = carriers.stream()
                .map(carrier -> CompletableFuture.supplyAsync(() -> carrier.quote(address), quoteExecutor))
                .toList();
            CompletableFuture.allOf(pendingQuotes.toArray(CompletableFuture[]::new))
                .get(policy.timeout().toMillis(), TimeUnit.MILLISECONDS);
            return pendingQuotes.stream().map(CompletableFuture::join).toList();
        } catch (RejectedExecutionException e) {
            throw new DeliveryQuoteUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeliveryQuoteUnavailableException(e);
        } catch (TimeoutException e) {
            // 늦은 택배사 하나 때문에 견적 화면이 끝없이 기다리지 않게 전체 시간을 끊고 견적 없음으로 알린다.
            // 남은 Future 는 취소하지 않는다. CompletableFuture.cancel 은 도는 호출을 멈추지 못하고, 호출 전체 시간은 ADR-018 의 응답 전체 타임아웃이 맡는다.
            throw new DeliveryQuoteTimeoutException(policy.timeout(), e);
        } catch (ExecutionException e) {
            throw new DeliveryQuoteUnavailableException(e.getCause());
        }
    }
}
