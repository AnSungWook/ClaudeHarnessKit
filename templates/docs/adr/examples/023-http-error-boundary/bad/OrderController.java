@RequiredArgsConstructor
@RestController
public class OrderController {

    private final OrderQueryService orderQueryService;

    @GetMapping("/orders/{orderId}")
    public OrderResult getOrder(@PathVariable long orderId) { // ADR-023 위반: 응답 DTO 가 아니라 서비스 결과를 그대로 내고, 경로 인자에 제약이 없다
        if (orderId <= 0) { // ADR-023 위반: 컨트롤러가 검증을 판단하고 상태를 정한다
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid orderId"); // ADR-023 위반: 컨트롤러에서 ResponseStatusException
        }
        try {
            return orderQueryService.getOrder(orderId);
        } catch (OrderNotFoundException e) { // ADR-023 위반: 사유 예외를 컨트롤러가 잡아 상태로 바꾼다 - 처리기 한 자리의 일이다
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order " + orderId);
        }
    }
}
