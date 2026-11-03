public interface OrderCancellationUseCase {

    // ADR-010 위반: 다른 기능이 부르는 공개 입구인데, 이름을 되풀이할 뿐 없을 때·출고 뒤·이미 취소됐을 때 무엇이 오는지 계약이 없다
    // 주문 취소
    OrderCancellationResult cancelOrder(long orderId);
}
