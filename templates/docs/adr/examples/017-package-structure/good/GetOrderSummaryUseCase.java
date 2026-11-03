/**
 * 다른 기능이 주문의 요약 값을 읽는 입구다. 주문의 영속 엔티티와 서비스 구현 대신 이것만 부른다.
 */
public interface GetOrderSummaryUseCase {

    /**
     * 주문 하나의 요약을 돌려준다.
     *
     * @param orderId 주문 ID
     * @return 주문 요약. null 을 돌려주지 않는다
     * @throws com.example.order.domain.exception.OrderNotFoundException 그 ID 의 주문이 없을 때(404)
     */
    OrderSummary getOrderSummary(long orderId);

    /**
     * 주문 여러 건의 요약을 한 번에 돌려준다. 행마다 getOrderSummary 를 부르지 않게 키를 모아 부른다.
     *
     * @param orderIds 주문 ID 목록. null 이면 안 되고, 비어 있으면 빈 목록을 돌려준다. 중복 ID 는 한 번만 담긴다
     * @return 불변 목록. 없는 ID 는 예외 없이 빠지고, 순서는 입력 순서와 상관없으니 {@link OrderSummary#orderId()} 로 짝짓는다
     */
    List<OrderSummary> getOrderSummaries(List<Long> orderIds);
}
