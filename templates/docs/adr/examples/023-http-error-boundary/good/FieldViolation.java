public record FieldViolation(String field, String reason) {

    private static final String REQUIRED = "필수 값입니다.";
    // ...

    // 프레임워크 예외 문구에는 클래스 이름과 내부 경로가 섞여 있어, 칸 이름만 꺼내고 사유는 검증 문구나 고정 문구를 쓴다.
    public static List<FieldViolation> listOf(Exception e) {
        return switch (e) {
            case MethodArgumentNotValidException invalid -> invalid.getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
            case MissingServletRequestParameterException missing ->
                List.of(new FieldViolation(missing.getParameterName(), REQUIRED));
            case InvalidValueException invalid -> List.of(new FieldViolation(invalid.getField(), invalid.getReason()));
            // ... HandlerMethodValidationException, MissingRequestHeaderException, MethodArgumentTypeMismatchException
            // 못 읽는 본문(깨진 JSON, 모르는 코드값)은 어느 칸인지 믿을 만하게 꺼낼 수 없어 본문 하나로 답한다.
            default -> List.of(new FieldViolation(BODY_FIELD, UNREADABLE_BODY));
        };
    }
}
