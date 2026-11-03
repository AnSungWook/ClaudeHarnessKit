public record DeliveryFee(long amount) {

    private static final long FREE_DELIVERY_THRESHOLD = 50_000;
    private static final long BASIC_FEE = 3_000;
    private static final long REMOTE_AREA_SURCHARGE = 3_000;

    public DeliveryFee {
        if (amount < 0) {
            throw new IllegalArgumentException("delivery fee must not be negative: %d".formatted(amount));
        }
    }

    public static DeliveryFee of(long orderAmount, DeliveryRegion region) {
        long basicFee = orderAmount >= FREE_DELIVERY_THRESHOLD ? 0 : BASIC_FEE;
        // 도서 산간 추가 요금은 택배사가 주문 금액과 상관없이 받는다. 그래서 무료 배송 기준을 넘어도 빼지 않는다.
        // TODO 추가 요금을 택배사별로 나눈다 - 두 번째 택배사 계약이 확정된 뒤(#142)
        long surcharge = region.isRemoteArea() ? REMOTE_AREA_SURCHARGE : 0;
        return new DeliveryFee(basicFee + surcharge);
    }
}
