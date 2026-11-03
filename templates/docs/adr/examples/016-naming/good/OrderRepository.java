public interface OrderRepository {

    Optional<Order> findById(long orderId);

    List<Order> findAllBy(OrderSearchCondition condition);

    void save(Order order);
}
