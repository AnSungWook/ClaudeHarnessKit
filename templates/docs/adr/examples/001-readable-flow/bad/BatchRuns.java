public BatchRun report(Long targetKey) {   // ADR-001 위반: 실행하지 않고 "할 일" 을 싸서 돌려준다
    ClaimedRun claimed = launcher.claim(targetKey)   // ADR-001 위반: claim - 무엇을 잡는지 이름에 없다
        .orElseThrow(ReportJobAlreadyRunningException::new);
    return BatchRun.holding(claimed.release(), lines ->   // ADR-001 위반: holding - 람다를 든 실행 객체를 돌려준다
        claimed.run().apply(new RunProgress(lines)));      // 진짜 잡 실행(jobOperator.start)은 ReportLauncher 의 Slot 안쪽에 있다
}
