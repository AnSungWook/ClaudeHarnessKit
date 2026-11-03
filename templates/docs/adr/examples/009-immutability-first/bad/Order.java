public class Order {

    private OrderStatus status;
    private final List<OrderLine> lines;
    private Optional<Instant> paidAt = Optional.empty(); // ADR-009 위반: Optional 필드 - null 과 빈 값, 두 가지 "없음" 이 생긴다
    // ...

    public Order(long id, long memberId, OrderStatus status, List<OrderLine> lines) {
        // ...
        this.lines = lines; // ADR-009 위반: 받은 리스트를 복사하지 않아, 넘겨준 쪽이 고치면 주문이 같이 바뀐다
    }

    public void setStatus(OrderStatus status) { // ADR-009 위반: public setter - 누가 언제 상태를 바꿨는지 추적할 수 없다
        this.status = status;
    }

    public void setPaidAt(Optional<Instant> paidAt) { // ADR-009 위반: Optional 매개변수, public setter
        this.paidAt = paidAt;
    }

    public List<OrderLine> getLines() {
        return lines; // ADR-009 위반: 내부 가변 리스트를 그대로 내보낸다
    }
}
