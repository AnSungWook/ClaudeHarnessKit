public interface OrderCancellationUseCase {

    /**
     * 주문을 취소하고 환불 예정 금액을 돌려준다. 회원 탈퇴, 결제 실패 처리 같은 다른 기능이 부르는 입구다.
     *
     * @param orderId 취소할 주문의 ID
     * @return 취소된 주문 ID 와 환불 예정 금액. 이미 취소된 주문이면 처음 취소한 결과를 그대로 돌려준다
     * @throws OrderNotFoundException 주문이 없을 때
     * @throws OrderAlreadyShippedException 이미 출고돼 취소할 수 없을 때
     */
    OrderCancellationResult cancelOrder(long orderId);
}
