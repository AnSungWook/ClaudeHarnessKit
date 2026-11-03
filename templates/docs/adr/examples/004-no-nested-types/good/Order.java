public record Order(long id, List<OrderLine> lines) {

    // ...

    public boolean hasBulkLine() {
        return lines.stream().anyMatch(OrderLine::isBulk);
    }
}
