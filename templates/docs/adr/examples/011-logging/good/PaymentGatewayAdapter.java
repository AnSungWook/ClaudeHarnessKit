@RequiredArgsConstructor
@Component
public class PaymentGatewayAdapter implements PaymentGateway {

    private final RestClient paymentGatewayRestClient;

    @Override
    public PaymentApproval approve(PaymentApprovalCommand command) {
        PaymentGatewayApproveResponse response = requestApproval(command);
        if (response == null) {
            throw new PaymentGatewayEmptyResponseException(command.paymentId());
        }
        return new PaymentApproval(response.approvalNumber(), response.approvedAt());
    }

    private PaymentGatewayApproveResponse requestApproval(PaymentApprovalCommand command) {
        try {
            return paymentGatewayRestClient.post().uri("/approvals").body(PaymentGatewayApproveRequest.of(command))
                .retrieve().body(PaymentGatewayApproveResponse.class);
        } catch (RestClientException e) {
            // 여기서 로그를 남기지 않는다. 감싸서 던지면 경계 처리기가 한 번 남긴다.
            throw new PaymentGatewayUnavailableException(command.paymentId(), e);
        }
    }
}
