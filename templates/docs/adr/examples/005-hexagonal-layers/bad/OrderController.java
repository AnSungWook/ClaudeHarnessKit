@RequestMapping("/orders")
@RestController
public class OrderController {

    // ...

    // ADR-005 위반: 영속 엔티티를 응답으로 그대로 낸다(칸을 바꾸면 API 계약이 같이 바뀐다)
    @GetMapping("/{orderId}")
    public CommonResponse<OrderEntity> getOrder(@PathVariable @Positive long orderId) {
        return CommonResponse.success(orderQueryService.getOrder(orderId));
    }
}
