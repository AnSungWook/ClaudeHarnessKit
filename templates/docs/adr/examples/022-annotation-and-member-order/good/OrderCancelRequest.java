public record OrderCancelRequest(
    @NotNull
    @JsonProperty("reason_code")
    OrderCancelReason reasonCode,
    @Size(max = OrderCancelRequest.MAX_MEMO_LENGTH)
    @JsonProperty("customer_memo")
    String memo) {

    static final int MAX_MEMO_LENGTH = 200;
}
