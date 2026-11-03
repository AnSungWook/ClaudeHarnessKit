public record Payment(long id, long orderId, long amount, PaymentStatus status) {

    public Payment completeWith(PaymentOutcome outcome) {
        return switch (outcome) {
            case PaymentOutcome.Approved approved -> withStatus(PaymentStatus.APPROVED);
            case PaymentOutcome.Declined(PaymentDeclineReason reason) when reason.isRetryable() ->
                withStatus(PaymentStatus.RETRY_WAITING);
            case PaymentOutcome.Declined declined -> withStatus(PaymentStatus.DECLINED);
        };
    }

    public Payment withStatus(PaymentStatus newStatus) {
        return new Payment(id, orderId, amount, newStatus);
    }
}
