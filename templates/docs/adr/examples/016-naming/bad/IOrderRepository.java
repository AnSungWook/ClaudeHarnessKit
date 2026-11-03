public interface IOrderRepository { // ADR-016 위반: 이름에 I 접두어(구현 표시)를 붙인다

    Optional<Order> getOrder(long id); // ADR-016 위반: 저장소 포트의 읽기가 find 가 아니라 get 이다

    List<Order> selectOrderList(OrderDto param); // ADR-016 위반: select 동사, List 접미, 조건인데 Condition 도 By 도 아니다

    void insertOrder(Order order); // ADR-016 위반: 저장소 포트의 쓰기가 save, update, delete 가 아니다
}
