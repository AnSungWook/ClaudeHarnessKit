@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse<T>(T result, ErrorDetail error, List<FieldViolation> errors, String traceId) {

    public CommonResponse {
        // 칸 검증이 없는 응답은 errors 칸을 아예 내지 않으려고 null 을 그대로 두고, 있으면 받은 목록과 끊는다.
        errors = errors == null ? null : List.copyOf(errors);
    }

    public static <T> CommonResponse<T> success(T result) {
        return new CommonResponse<>(result, null, null, null);
    }

    public static CommonResponse<Void> failure(CustomErrorCode errorCode, String traceId) {
        return new CommonResponse<>(null, new ErrorDetail(errorCode.getCode(), errorCode.getMessage()), null, traceId);
    }

    public static CommonResponse<Void> invalid(List<FieldViolation> violations, String traceId) {
        return new CommonResponse<>(null, null, violations, traceId);
    }
}
