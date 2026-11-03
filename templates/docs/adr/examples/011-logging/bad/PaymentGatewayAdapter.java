@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentGatewayAdapter implements PaymentGateway {

    // ...

    private PaymentGatewayApproveResponse requestApproval(PaymentApprovalCommand command) {
        PaymentGatewayApproveRequest request = PaymentGatewayApproveRequest.of(command);
        try {
            return paymentGatewayRestClient.post().uri("/approvals").body(request)
                .retrieve().body(PaymentGatewayApproveResponse.class);
        } catch (RestClientException e) {
            e.printStackTrace(); // ADR-011 위반: printStackTrace - 로그 저장소에 안 남고 수준도 없다
            log.error("approve failed: request={}", request, e); // ADR-011 위반: 카드번호가 든 요청 본문과 원인 문구를 남기고, 다시 던져 경계에서 한 번 더 찍힌다
            throw new PaymentGatewayUnavailableException(command.paymentId(), e);
        }
    }
}
