public record OrderLines(List<OrderLine> lines) {

    private static final String PRODUCT_NAME_DELIMITER = ", ";

    // ...

    public List<OrderLine> couponApplicableLines() {
        return lines.stream()
            .filter(OrderLine::isCouponApplicable)
            .toList();
    }

    public Map<ProductCategory, Long> amountByCategory() {
        return lines.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.groupingBy(OrderLine::category, Collectors.summingLong(OrderLine::amount)),
                Map::copyOf));
    }

    public String joinedProductNames() {
        return lines.stream()
            .map(OrderLine::productName)
            .collect(Collectors.joining(PRODUCT_NAME_DELIMITER));
    }
}
