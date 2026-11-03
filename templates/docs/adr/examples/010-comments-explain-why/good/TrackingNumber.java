public record TrackingNumber(String value) {

    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d{10,14}");

    public TrackingNumber {
        if (value == null || !DIGITS_ONLY.matcher(value).matches()) {
            throw new IllegalArgumentException("tracking number must be 10 to 14 digits: %s".formatted(value));
        }
    }

    public static TrackingNumber of(String enteredValue) {
        // 택배사 화면은 하이픈을 넣어 보여 주지만 조회 API 는 숫자만 받는다. 화면에서 복사해 넣은 값도 받으려고 여기서 뺀다.
        return new TrackingNumber(enteredValue.strip().replace("-", ""));
    }
}
