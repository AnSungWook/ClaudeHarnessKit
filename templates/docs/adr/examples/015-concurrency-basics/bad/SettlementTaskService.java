@RequiredArgsConstructor
@Service
public class SettlementTaskService {

    private final SettlementTaskRepository settlementTasks;

    @Transactional
    public synchronized List<SettlementTask> startPendingTasks(int limit) { // ADR-015 위반: 여러 서버가 같이 쓰는 DB 행을 JVM 잠금으로 지킨다 - 서버가 둘이면 같은 건을 둘 다 잡는다
        List<SettlementTask> startedTasks = settlementTasks.findPending(limit).stream()
            .map(SettlementTask::start)
            .toList();
        startedTasks.forEach(settlementTasks::save);
        return startedTasks;
    }
}
