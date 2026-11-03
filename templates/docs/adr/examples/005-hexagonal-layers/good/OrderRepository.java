public interface OrderRepository {

    Optional<Order> findById(long orderId);

    Order save(Order order);
}
