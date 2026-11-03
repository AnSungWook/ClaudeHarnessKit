@RequiredArgsConstructor
@Repository
public class JdbcMemberRepository implements MemberRepository {

    private static final String FIND_BY_EMAIL = """
        select id, email, name
          from member
         where email = :email
        """;

    private final JdbcClient jdbcClient;

    @Override
    public Optional<Member> findByEmail(String email) {
        try {
            return jdbcClient.sql(FIND_BY_EMAIL)
                .param("email", email)
                .query((row, rowNumber) -> new Member(row.getLong("id"), row.getString("email"), row.getString("name")))
                .optional();
        } catch (DataAccessException e) {
            // 이메일은 개인정보라 사유 예외 문구에 싣지 않는다. 원인만 단다.
            throw new MemberStorageUnavailableException(e);
        }
    }
}
