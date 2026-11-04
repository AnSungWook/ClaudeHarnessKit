// point/adapter/in/event - 다른 기능의 이벤트를 받는다. 이벤트 타입은 order/domain 의 공개 값(ADR-017).
// 리스너는 유스케이스에 넘기기만 한다. 실패를 어떻게 다루는지는 유스케이스 구현(아래 PointAccrualService)이 든다.
@Component
public class PointAccrualOnOrderPlacedListener {

    private final AccruePointUseCase accruePoint;   // point 의 port/in

    // ...

    @Async("pointExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void accruePointOnOrderPlaced(OrderPlacedEvent event) {
        accruePoint.accrueFor(event.orderId(), event.memberKey());
    }
}

// point/application/service - 실패 경로를 정한 자리.
@Service
public class PointAccrualService implements AccruePointUseCase {

    private final MemberPoints memberPoints;          // out 포트
    private final PointAccrualFailures accrualFailures; // out 포트 - 실패 기록, 재적립 배치가 읽는다

    // ...

    @Override
    public void accrueFor(long orderId, long memberKey) {
        try {
            // 넘겨받은 memberKey 는 권한의 증명이 아니다 - 회원 상태와 적립 대상인지를 여기서 다시 확인한다.
            memberPoints.accrue(PointAccrual.of(orderId, memberPoints.getActiveMember(memberKey)));
        } catch (PointAccrualFailedException failure) {
            // 실패를 격리하되 삼키지 않는다: 누가 다시 하는지(재적립 배치)와 운영 확인(로그 한 번, ADR-011)을 정했다.
            accrualFailures.recordForRetry(orderId, failure.reason());
            log.warn("point accrual failed orderId={} reason={}", orderId, failure.reason());
        }
    }
}
