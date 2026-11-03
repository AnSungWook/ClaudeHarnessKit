@Service
public class DeliveryQuoteService {

    private final List<CarrierQuoteSource> carriers;
    private DeliveryAddress currentAddress; // ADR-015 위반: 싱글턴 빈에 요청마다 바뀌는 필드 - 동시 요청끼리 주소가 섞인다
    // ... 생성자로 carriers 를 받는다

    public List<DeliveryQuote> getQuotes(DeliveryAddress address) {
        this.currentAddress = address;
        List<CompletableFuture<DeliveryQuote>> pendingQuotes = carriers.stream()
            .map(carrier -> CompletableFuture.supplyAsync(() -> carrier.quote(currentAddress))) // ADR-015 위반: 실행기 없이 공용 풀 - 느린 택배사가 앱 전체의 공용 풀을 막는다
            .toList();
        return pendingQuotes.stream().map(this::waitForQuote).toList();
    }

    private DeliveryQuote waitForQuote(CompletableFuture<DeliveryQuote> pendingQuote) {
        try {
            return pendingQuote.get();
        } catch (InterruptedException e) {
            throw new DeliveryQuoteUnavailableException(e); // ADR-015 위반: 중단 표시를 되살리지 않는다 - 위에서 종료 신호를 알 길이 없다
        } catch (ExecutionException e) {
            throw new DeliveryQuoteUnavailableException(e.getCause());
        }
    }
}
