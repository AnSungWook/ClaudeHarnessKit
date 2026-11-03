@RequestMapping("/orders")
@RestController
public class OrderController {

    // ...

    @GetMapping("/{orderId}")
    public CommonResponse<OrderResponse> getOrder(@PathVariable @Positive long orderId) {
        return CommonResponse.success(OrderResponse.of(orderQueryService.getOrder(orderId)));
    }
}
