// ADR-006 위반: 도메인 부모 OrderException 을 상속해 공통 형제 ConflictException 을 못 탄다(단일 상속) - 경계가 409 로 못 바꾼다
public class OrderAlreadyPaidException extends OrderException {

    // ADR-006 위반: 예외마다 code 와 사용자 문구 상수를 따로 둔다(도메인 ErrorCode enum 한 자리가 든다)
    public static final String CODE = "ORDER_ALREADY_PAID";
    public static final String USER_MESSAGE = "이미 결제된 주문입니다.";

    public OrderAlreadyPaidException(long orderId) {
        super(CODE, USER_MESSAGE, "Order already paid: orderId=%d".formatted(orderId));
    }
}
