@Service
public class OrderCancellationService {

    // ADR-008 위반: 필드 주입 - final 이 아니고, 의존이 빈 채로 객체가 만들어질 수 있다
    @Autowired
    private OrderRepository orders;

    private Clock clock;

    // ADR-008 위반: setter 주입 - 생성 뒤에 바뀔 수 있고, 테스트가 이 호출을 빠뜨리면 NullPointerException
    @Autowired
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    // ...
}
