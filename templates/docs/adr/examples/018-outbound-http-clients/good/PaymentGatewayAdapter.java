public class PaymentGatewayAdapter implements PaymentGateway {

    // ...

    // 상태 조회는 몇 번 가도 결과가 같아, 바깥이 응하지 않은 실패만 다시 보낸다.
    @Retryable(includes = PaymentGatewayUnavailableException.class, maxRetries = 2, delay = 200)
    @Override
    public PaymentStatus checkPaymentStatus(String paymentKey) {
        try {
            PaymentGatewayStatusResponse response = paymentGatewayRestClient.get() // ...
                .retrieve().body(PaymentGatewayStatusResponse.class);
            return requireComplete(paymentKey, response).toPaymentStatus();
        } catch (RestClientException e) {
            throw translate(paymentKey, e);
        }
    }

    // ... requireComplete: 빈 본문이나 빠진 필수 칸을 PaymentGatewayInvalidResponseException 으로 막는다(ADR-013)

    private RuntimeException translate(String paymentKey, RestClientException e) {
        return switch (e) {
            case HttpClientErrorException.TooManyRequests tooManyRequests -> new PaymentGatewayUnavailableException(paymentKey, e);
            case HttpClientErrorException clientError ->
                new PaymentGatewayRejectedException(paymentKey, clientError.getStatusCode().value(), e);
            // 5xx, 연결 실패, 읽기 타임아웃은 요청이 틀린 것이 아니라 바깥이 응하지 않은 것이다.
            default -> new PaymentGatewayUnavailableException(paymentKey, e);
        };
    }

    // 승인은 두 번 가면 두 번 결제될 수 있어 재시도하지 않는다. 결과를 모르면 호출한 쪽이 checkPaymentStatus 로 확인한다.
    @Override
    public PaymentApproval approvePayment(Payment payment) {
        // ... 같은 클라이언트로 POST 하고, 실패는 translate 로 감싼다
    }
}
