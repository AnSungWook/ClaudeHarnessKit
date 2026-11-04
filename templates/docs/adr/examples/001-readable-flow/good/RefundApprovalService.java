// order/application/service - 동작을 바꾸는 advice 는 적용되는 메서드에 표시가 보이고, 이름에서 무엇을 하는지 읽힌다(ADR-001).
@Service
public class RefundApprovalService implements ApproveRefundUseCase {

    // ...

    @Override
    @PreAuthorize("hasRole('REFUND_APPROVER')")   // 권한: 이 메서드에 보인다
    @Transactional                                 // 트랜잭션: 서비스 메서드에만(ADR-014)
    public ApprovedRefund approveRefund(ApproveRefundCommand command) {
        Refund refund = refunds.findById(command.refundKey()).orElseThrow(() -> new RefundNotFoundException(command.refundKey()));
        return ApprovedRefund.of(refunds.save(refund.approve(command.approverKey(), clock)));
    }
}
