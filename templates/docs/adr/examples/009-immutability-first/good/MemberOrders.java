public record MemberOrders(List<Order> orders) {

    public MemberOrders {
        orders = List.copyOf(orders);
    }

    public List<Order> unpaidOrders() {
        return orders.stream()
            .filter(order -> !order.isPaid())
            .toList();
    }
}
