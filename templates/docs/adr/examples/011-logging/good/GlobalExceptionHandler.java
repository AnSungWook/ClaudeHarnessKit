// 상태는 ADR-023 매핑 표의 갈래를 빠짐없이 따른다. 이 예시의 주제는 로그 자리이고, 본문(errors[], traceId)을 만드는 모양은 ADR-023 예시가 든다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
        MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, InvalidValueException.class})
    public ResponseEntity<CommonResponse<Void>> handleInvalidRequest(Exception e) {
        log.warn("Invalid request: errorType={}", e.getClass().getSimpleName(), LogSafeException.of(e));
        return ResponseEntity.badRequest().body(CommonResponse.invalid(FieldViolation.listOf(e), currentTraceId()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = switch (e) {
            case ForbiddenException forbidden -> HttpStatus.FORBIDDEN;
            case NotFoundException notFound -> HttpStatus.NOT_FOUND;
            case ConflictException conflict -> HttpStatus.CONFLICT;
            case UnavailableException unavailable -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.warn("Business exception: status={}, errorCode={}, message={}",
            status.value(), e.getErrorCode().getCode(), e.getMessage(), LogSafeException.of(e));
        return ResponseEntity.status(status).body(CommonResponse.failure(e.getErrorCode(), currentTraceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected error: errorType={}", e.getClass().getSimpleName(), LogSafeException.of(e));
        return ResponseEntity.internalServerError()
            .body(CommonResponse.failure(CommonErrorCode.INTERNAL_ERROR, currentTraceId()));
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
