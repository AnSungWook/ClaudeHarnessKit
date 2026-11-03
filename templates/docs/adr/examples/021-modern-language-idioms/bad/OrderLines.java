public record OrderLines(List<OrderLine> lines) {

    public List<OrderLine> couponApplicableLines() {
        List<OrderLine> result = new ArrayList<>(); // ADR-021 위반: 결과를 쌓아 돌려주는 for + new ArrayList
        for (OrderLine line : lines) {
            if (line.isCouponApplicable()) {
                result.add(line);
            }
        }
        return result;
    }

    public Map<ProductCategory, Long> amountByCategory() {
        Map<ProductCategory, Long> amounts = new HashMap<>();
        lines.stream().forEach(line -> amounts.merge(line.category(), line.amount(), Long::sum)); // ADR-021 위반: 스트림 forEach 안에서 바깥 맵을 고친다
        return amounts;
    }

    public String joinedProductNames() {
        String names = "";
        for (int i = 0; i < lines.size(); i++) {
            names = names + (i == 0 ? "" : ", ") + lines.get(i).productName(); // ADR-021 위반: 잇기를 + 로 조립한다(Collectors.joining 자리)
        }
        return names;
    }
}
