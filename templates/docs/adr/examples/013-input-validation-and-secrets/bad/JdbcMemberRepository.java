@RequiredArgsConstructor
@Repository
public class JdbcMemberRepository implements MemberRepository {

    private final JdbcClient jdbcClient;

    @Override
    public Optional<Member> findByEmail(String email) {
        try {
            return jdbcClient.sql("select id, email, name from member where email = '" + email + "'") // ADR-013 위반: 사용자 값을 SQL 에 이어 붙인다 - "' or '1'='1" 한 줄로 모든 회원이 나온다
                .query((row, rowNumber) -> new Member(row.getLong("id"), row.getString("email"), row.getString("name")))
                .optional();
        } catch (DataAccessException e) {
            throw new MemberStorageUnavailableException(e);
        }
    }
}
