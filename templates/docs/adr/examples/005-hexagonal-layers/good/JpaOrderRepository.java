@Repository
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    // ...

    @Override
    public Optional<Order> findById(long orderId) {
        return orderJpaRepository.findById(orderId).map(OrderEntity::toOrder);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(OrderEntity.of(order)).toOrder();
    }
}
