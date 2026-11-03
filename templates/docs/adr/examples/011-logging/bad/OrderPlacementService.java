@Slf4j
@RequiredArgsConstructor
@Service
public class OrderPlacementService {

    private final OrderRepository orders;
    private final Clock clock;

    @Transactional
    public OrderPlacementResult placeOrder(PlaceOrderCommand command) {
        System.out.println("주문 시작"); // ADR-011 위반: System.out - 로그 수준도, 검색도 안 된다
        log.info("placeOrder request=" + command); // ADR-011 위반: 문자열 + 연결, 요청을 통째로(연락처·주소가 실린다)
        try {
            Order savedOrder = orders.save(Order.place(command.memberId(), command.lines(), clock.instant()));
            log.error("주문 생성 orderId={}", savedOrder.id()); // ADR-011 위반: 업무 사건을 ERROR 로 - 조치할 일이 없는데 경보가 울린다
            return OrderPlacementResult.of(savedOrder);
        } catch (RuntimeException e) {
            log.error("주문 실패", e); // ADR-011 위반: 찍고 다시 던진다 - 경계 처리기가 또 찍어 같은 실패가 두 번 보인다
            throw e;
        }
    }
}
