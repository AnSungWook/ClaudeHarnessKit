public enum OrderErrorCode implements CustomErrorCode {
    ORDER_NOT_FOUND("주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID("이미 결제된 주문입니다."),
    ORDER_RECEIPT_STORAGE_UNAVAILABLE("영수증을 저장하지 못했습니다. 잠시 뒤 다시 시도해 주세요.");

    private final String message;

    // ...

    @Override
    public String getCode() {
        return name();
    }

    // ...
}
