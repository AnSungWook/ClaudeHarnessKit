// 결과 갈래는 바깥 타입과 같이 읽어야 뜻이 서서 중첩으로 둔다(ADR-004 의 허용 예, 허용 목록에 이유를 적는다).
public sealed interface PaymentOutcome permits PaymentOutcome.Approved, PaymentOutcome.Declined {

    record Approved(String approvalNumber, Instant approvedAt) implements PaymentOutcome {
        public Approved {
            Objects.requireNonNull(approvalNumber, "approvalNumber");
            Objects.requireNonNull(approvedAt, "approvedAt");
        }
    }

    record Declined(PaymentDeclineReason reason) implements PaymentOutcome {
        public Declined {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
