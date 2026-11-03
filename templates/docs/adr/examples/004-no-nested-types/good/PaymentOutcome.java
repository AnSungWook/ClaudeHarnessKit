// 갈래는 PaymentOutcome 없이는 뜻이 없고, 늘 switch 로 함께 읽힌다(ADR-004 의 허용 예). 아키텍처 테스트 허용 목록에도 이 이유로 적는다.
public sealed interface PaymentOutcome {

    record Approved(String approvalNumber, Instant approvedAt) implements PaymentOutcome {
        // ...
    }

    record Declined(PaymentDeclineReason reason) implements PaymentOutcome {
        // ...
    }
}
