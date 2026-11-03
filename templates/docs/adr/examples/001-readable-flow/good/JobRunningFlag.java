// 배치는 서버 한 대에서 이 창구로만 돈다. 그래서 JVM 안의 표시로 겹침을 막는다(ADR-015).
private final AtomicBoolean running = new AtomicBoolean();

public void markStartedOrReject() {
    if (!running.compareAndSet(false, true)) {
        throw new ReportJobAlreadyRunningException();
    }
}

public void markFinished() {
    running.set(false);
}
