@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
        MissingServletRequestParameterException.class, MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, InvalidValueException.class})
    public ResponseEntity<CommonResponse<Void>> handleInvalidRequest(Exception e) {
        log.warn("Invalid request: exception={}", e.getClass().getSimpleName(), LogSafeException.of(e));
        return ResponseEntity.badRequest().body(CommonResponse.invalid(FieldViolation.listOf(e), currentTraceId()));
    }

    // ... currentTraceId(): 로그 패턴이 찍는 추적 ID 와 같은 값(MDC 의 traceId)을 응답에 싣는다(ADR-011)

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("Business failure: code={}, message={}", e.getErrorCode().getCode(), e.getMessage(), LogSafeException.of(e));
        return ResponseEntity.status(statusOf(e)).body(CommonResponse.failure(e.getErrorCode(), currentTraceId()));
    }

    private static HttpStatus statusOf(BusinessException e) {
        return switch (e) {
            case ForbiddenException forbidden -> HttpStatus.FORBIDDEN;
            case NotFoundException notFound -> HttpStatus.NOT_FOUND;
            case ConflictException conflict -> HttpStatus.CONFLICT;
            case UnavailableException unavailable -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", LogSafeException.of(e));
        // 원인 문구에 SQL, 클래스 이름, 경로가 섞일 수 있어 응답에는 고정 code 와 고정 문구만 싣는다.
        return ResponseEntity.internalServerError()
            .body(CommonResponse.failure(CommonErrorCode.INTERNAL_ERROR, currentTraceId()));
    }
}
