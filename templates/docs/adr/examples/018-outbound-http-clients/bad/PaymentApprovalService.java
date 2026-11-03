@RequiredArgsConstructor
@Service
public class PaymentApprovalService {

    private final PaymentRepository payments;

    @Retryable(maxRetries = 3) // ADR-018 위반: 두 번 가면 두 번 결제되는 승인에 재시도를 걸고, 실패 종류(4xx, 5xx, 429)도 가르지 않는다
    public PaymentResult approvePayment(long paymentId) {
        Payment payment = payments.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        RestClient restClient = RestClient.create("https://pg.example.com"); // ADR-018 위반: 서비스가 클라이언트를 만들고, 타임아웃이 라이브러리 기본값이다
        Payment approved = restClient.post()
            .uri("/v1/payments/confirm")
            .body(payment) // ADR-018 위반: 도메인 값을 바깥 본문으로 그대로 직렬화한다
            .retrieve()
            .body(Payment.class); // ADR-018 위반: 바깥 응답을 도메인 타입으로 바로 받고, RestClientException 이 서비스 밖으로 그대로 올라간다
        return PaymentResult.of(payments.save(approved));
    }
}
