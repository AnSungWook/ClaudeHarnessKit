public interface OrderRepository {

    Optional<Order> findById(long orderId);

    /**
     * 회원의 주문을 돌려준다. 없으면 빈 리스트이고 null 은 돌려주지 않는다.
     */
    List<Order> findAllByMemberId(long memberId);

    Order save(Order order);
}
