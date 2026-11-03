@RequiredArgsConstructor
@CrossOrigin(origins = "*") // ADR-019 위반: 모든 origin 에 연다
@RestController
public class OrderController {

    private static final String INTERNAL_TOKEN = "s3cr3t-internal-token"; // ADR-019 위반(ADR-013 도): 토큰이 코드에 박혀 있다

    private final OrderQueryService orderQueryService;

    @GetMapping("/api/orders/{orderId}")
    public ResponseEntity<CommonResponse<OrderResponse>> getOrder(
        @RequestHeader("X-Internal-Token") String token, @PathVariable @Positive long orderId) {
        if (!INTERNAL_TOKEN.equals(token)) { // ADR-019 위반: 컨트롤러마다 인증을 검사해, 한 곳을 고쳐도 다른 창구가 열린 채 남는다
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // ADR-019 위반: 401 이 공통 오류 본문(ADR-023)이 아니다
        }
        return ResponseEntity.ok(CommonResponse.success(OrderResponse.of(orderQueryService.getOrder(orderId))));
    }
}
