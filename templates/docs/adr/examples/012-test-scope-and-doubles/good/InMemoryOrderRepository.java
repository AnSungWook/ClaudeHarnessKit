class InMemoryOrderRepository implements OrderRepository {

    // 테스트 메서드 하나가 만들어 혼자 쓰는 가짜라 동시 접근을 막지 않는다.
    private final List<Order> savedOrders = new ArrayList<>();

    @Override
    public Optional<Order> findById(long orderId) {
        return savedOrders.stream().filter(order -> order.id() == orderId).findFirst();
    }

    @Override
    public Order save(Order order) {
        savedOrders.add(order);
        return order;
    }

    List<Order> savedOrders() {
        return List.copyOf(savedOrders);
    }
}
