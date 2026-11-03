@RequiredArgsConstructor
@Component
public class PaymentGatewayAdapter implements PaymentGateway {

    private static final String FALLBACK_API_KEY = "pg-live-key-0000"; // ADR-013 위반: 비밀값이 코드에 - 저장소 이력에 영영 남는다

    private final RestClient paymentGatewayRestClient;

    @Override
    public PaymentApproval approve(PaymentApprovalCommand command) {
        String apiKey = System.getenv("PAYMENT_GATEWAY_API_KEY"); // ADR-013 위반: 설정 객체 밖에서 System.getenv - 받는 자리가 흩어지고 기동 때 검사가 없다
        if (apiKey == null) {
            apiKey = FALLBACK_API_KEY; // ADR-013 위반: 필수 비밀값이 없는데 기동이 실패하지 않고 기본값으로 돈다
        }
        // ... apiKey 를 Authorization 헤더에 실어 결제사를 부르고, 빈 응답과 RestClientException 은 사유 예외로 바꾼다
    }
}
