@RequiredArgsConstructor
@RequestMapping("/orders")
@RestController
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    // ...

    @GetMapping
    public CommonResponse<OrderPageResponse> getOrders(
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        OrderSearchCondition condition = new OrderSearchCondition(page, size);
        return CommonResponse.success(OrderPageResponse.of(orderQueryService.getOrdersBy(condition)));
    }

    @PostMapping("/{orderId}/cancellations")
    public CommonResponse<OrderResponse> cancelOrder(
        @PathVariable @Positive long orderId, @Valid @RequestBody OrderCancelRequest request) {
        OrderCancelCommand command = new OrderCancelCommand(orderId, request.reasonCode(), request.memo());
        return CommonResponse.success(OrderResponse.of(orderCancellationService.cancelOrder(command)));
    }
}
