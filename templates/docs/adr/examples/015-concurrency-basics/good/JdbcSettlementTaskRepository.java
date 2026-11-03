@RequiredArgsConstructor
@Repository
public class JdbcSettlementTaskRepository implements SettlementTaskRepository {

    // 여러 서버가 같은 표에서 일을 나눠 가져간다. 잠긴 행은 건너뛰어 같은 건을 둘이 잡지도, 서로 기다리지도 않게 한다.
    // 잠금은 부르는 서비스의 트랜잭션이 끝날 때 풀리므로, 서비스는 가져온 건을 처리 중으로 바꾸고 바로 커밋한다.
    private static final String FIND_AND_LOCK_PENDING = """
        select id, merchant_id, settlement_date
          from settlement_task
         where status = :status
         order by id
         limit :limit
           for update skip locked
        """;

    private final JdbcClient jdbcClient;

    @Override
    public List<SettlementTask> findAndLockPending(int limit) {
        try {
            return List.copyOf(jdbcClient.sql(FIND_AND_LOCK_PENDING)
                .param("status", SettlementTaskStatus.PENDING.name())
                .param("limit", limit)
                .query(SettlementTask.class)
                .list());
        } catch (DataAccessException e) {
            throw new SettlementTaskStorageUnavailableException(e);
        }
    }
}
