public record PaymentGatewayStatusResponse(
    @JsonProperty("payment_key")
    String paymentKey,
    @JsonProperty("status_code")
    PaymentGatewayStatusCode statusCode) implements PaymentGatewayResponse {

    @Override
    public boolean hasRequiredFields() {
        return paymentKey != null && statusCode != null;
    }

    public PaymentStatus toPaymentStatus() {
        return statusCode.toPaymentStatus();
    }
}
