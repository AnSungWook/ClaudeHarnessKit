public record DeliveryFee(long amount) {

    private static final long FREE_DELIVERY_THRESHOLD = 50_000;
    private static final long BASIC_FEE = 3_000;
    private static final long REMOTE_AREA_SURCHARGE = 3_000;

    // ADR-010 위반: 바뀐 이력과 작성자는 커밋 메시지가 든다
    // 2026-03-02 홍길동 수정 - 도서 산간 추가 요금 반영
    public static DeliveryFee of(long orderAmount, DeliveryRegion region) {
        // ADR-010 위반: 코드를 그대로 풀어 쓴 주석 - 기준 금액이 바뀌면 거짓말이 된다
        // 주문 금액이 5만 원 이상이면 기본 요금은 0원
        long basicFee = orderAmount >= FREE_DELIVERY_THRESHOLD ? 0 : BASIC_FEE;
        // ADR-010 위반: 무엇을 하는지만 적고, 왜 무료 배송이어도 붙는지(택배사 계약)는 없다
        // 도서 산간이면 추가 요금을 더한다
        long surcharge = region.isRemoteArea() ? REMOTE_AREA_SURCHARGE : 0;
        // ADR-010 위반: 지운 코드를 주석으로 남겼다 - git 에 있다
        // if (orderAmount >= FREE_DELIVERY_THRESHOLD) { surcharge = 0; }
        // ADR-010 위반: 무엇을 왜 미뤘는지가 없다 - 읽는 사람이 무엇을 고쳐야 하는지, 언제 고치는지 모른다
        // TODO 고칠 것
        return new DeliveryFee(basicFee + surcharge);
    }
}
