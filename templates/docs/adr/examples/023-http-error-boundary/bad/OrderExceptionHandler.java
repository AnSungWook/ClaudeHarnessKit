@Slf4j
@RestControllerAdvice(assignableTypes = OrderController.class) // ADR-023 위반: 창구마다 처리기를 따로 둔다
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotCancellableException.class)
    public ProblemDetail handleNotCancellable(OrderNotCancellableException e) { // ADR-023 위반: 이 창구만 ProblemDetail, 다른 창구는 다른 본문
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()); // ADR-023 위반: 로그용 내부 문구가 응답으로 나간다
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
            .body(Map.of("error", String.valueOf(e.getMessage()))); // ADR-023 위반: 500 본문에 예외 문구가 실리고 traceId 가 없다
    }
}
