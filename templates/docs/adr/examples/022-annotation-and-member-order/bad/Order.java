public final class Order {

    // ... 필드와 private 생성자

    public long totalAmount() { /* ... */ } // ADR-022 위반: 엔티티 자리 순서가 계산된 값, 판단, 상태 변경으로 뒤집혀 있다

    public boolean isCancellable() { /* ... */ }

    public Order cancel() {
        validateCancellable();
        return new Order(id, memberId, lines, OrderStatus.CANCELED);
    }

    public OrderResponse toResponse() { // ADR-022 위반(ADR-002, ADR-005 도): 엔티티가 응답 타입으로 바꾼다 - to... 는 도메인 값 변환만
        return OrderResponse.of(this);
    }

    private void validateCancellable() { // ADR-022 위반: cancel 이 부르는 private 이 cancel 아래가 아니라 파일 끝에 있다
        // ...
    }
}
