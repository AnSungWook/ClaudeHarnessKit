# ADR-023 예시: HTTP 오류는 처리기 한 자리가 매핑 표 하나로, 컨트롤러는 바꾸고 부르고 바꾸기만

[ADR-023](../../ADR-023_http-error-boundary.md) 을 주문 조회·취소 창구와 공통 예외 처리기에 적용한 예다. 오류 본문은 이 킷의 공통 응답 본문이다(`ProblemDetail` 이 아니다).

## 무엇을 보여 주나

사유 예외를 HTTP 상태와 본문으로 바꾸는 일은 `ApiExceptionHandler` 한 자리의 `statusOf` 표 하나가 한다. 모르는 예외는 고정 code 와 고정 문구만 낸다.
컨트롤러는 요청을 바꾸고, 서비스를 한 번 부르고, 응답 DTO 로 바꾼다. 형식 검증은 `@Valid` 와 인자 제약이 하고, 컨트롤러에는 분기가 없다.

본문 모양은 이렇다(`null` 칸은 안 나간다).

```json
{ "result": { "orderId": 7, "status": "CANCELED" } }
{ "error": { "errorCode": "ORDER_NOT_CANCELLABLE", "errorMessage": "취소할 수 없는 주문입니다." }, "traceId": "4bf92f3577b34da6" }
{ "errors": [ { "field": "size", "reason": "100 이하여야 합니다" } ], "traceId": "4bf92f3577b34da6" }
```

## 바른 모양

- [`good/CommonResponse.java`](good/CommonResponse.java) - 공통 응답 본문. 성공은 `result`, 오류는 `error{errorCode, errorMessage}`, 칸 검증은 `errors[]{field, reason}`, 오류 응답이면 최상위 `traceId`. `errorCode`, `errorMessage` 는 ErrorCode 의 `getCode()`, `getMessage()` 다(ADR-006). `ErrorDetail(errorCode, errorMessage)` 은 판단 없는 record 다.
- [`good/FieldViolation.java`](good/FieldViolation.java) - 칸 검증 한 줄. 프레임워크 예외에서 칸 이름만 꺼내고 사유는 검증 문구나 고정 문구로 둔다. 판단은 이 타입의 팩토리 `listOf(e)` 가 든다(ADR-002).
- [`good/ApiExceptionHandler.java`](good/ApiExceptionHandler.java) - 저장소에 하나뿐인 `@RestControllerAdvice`. 요청 칸 검증 묶음과 `InvalidValueException` 은 400 `errors[]`, `BusinessException` 갈래는 `statusOf` 표로 403·404·409·503·400, 그 밖 전부는 500 고정 문구. 로그는 여기서 한 번(4xx·503 은 WARN, 500 은 스택과 ERROR).
  - 500 로그에는 원본 예외 대신 `LogSafeException.of(e)` 를 넘긴다. 사슬 단계마다 메시지를 빼고 클래스 이름만 남기며 스택 위치는 그대로라, SQL 바인딩 값이나 토큰이 로그에 실리지 않는다(ADR-011). 이 도우미는 [011 예시](../011-logging/README.md)가 정의하고 여기서는 이름으로만 쓴다.
- `CommonResponse` 는 compact 생성자에서 `errors` 를 `List.copyOf` 로 끊는다(ADR-009). 칸 검증이 없는 응답은 `errors` 를 `null` 로 두어 JSON 에서 뺀다.
- [`good/OrderController.java`](good/OrderController.java) - 경로 kebab-case 복수 명사(`/orders/{orderId}/cancellations`), `@PathVariable`·`@RequestParam` 제약, 페이지 크기 상한, `@Valid @RequestBody`. 클래스에 `@Validated` 를 달지 않는다(Spring 6.1 이상 메서드 검증 내장).

## 어긋난 모양

- [`bad/OrderController.java`](bad/OrderController.java) - 컨트롤러가 값을 검사해 `ResponseStatusException` 을 던지고, 사유 예외를 직접 잡아 상태로 바꾸고, 서비스 결과를 그대로 낸다.
- [`bad/OrderExceptionHandler.java`](bad/OrderExceptionHandler.java) - 창구 전용 처리기(`assignableTypes`), 이 창구만 `ProblemDetail`, 내부 문구와 500 예외 문구가 응답으로 나가고 `traceId` 가 없다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@RestControllerAdvice(assignableTypes = OrderController.class)` | `ApiExceptionHandler` 하나와 `statusOf` 표 하나 | `@RestControllerAdvice` 가 저장소에 하나다 |
| `throw new ResponseStatusException(NOT_FOUND, "order " + id)` | 서비스가 `OrderNotFoundException`(→ `NotFoundException` → 404) | 컨트롤러에 `ResponseStatusException` 이 없다 |
| 500 본문 `{"error": e.getMessage()}` | `CommonErrorCode.INTERNAL_ERROR` 고정 code·문구, 원인은 `LogSafeException.of(e)` 로 가린 ERROR 로그 | 500 응답 본문에 예외 메시지, 클래스 이름, 스택이 없다 |
| `ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage())` | `CommonResponse.failure(e.getErrorCode(), traceId)` - 사용자 문구는 ErrorCode 의 message | 오류 본문에 `traceId` 가 있고 로그에 남는 추적 ID 와 같다 / (Decision: 오류 본문은 공통 응답 본문 하나, 내부 문구는 로그로만) |
| `if (orderId <= 0) throw ...` | `@PathVariable @Positive long orderId` | `@RequestParam`·`@PathVariable` 에 제약이 있다 / 컨트롤러 메서드에 분기가 없다 |
| `public OrderResult getOrder(...)` | `CommonResponse<OrderResponse>` 로 `OrderResponse.of(...)` | 컨트롤러가 엔티티나 도메인 값을 그대로 내지 않는다 |
| 크기 상한 없는 목록 | `@Min(1) @Max(MAX_PAGE_SIZE) int size` | (Decision: 목록은 크기에 상한) |

- 로그 칸은 표의 기본값이다. 설정이나 데이터가 틀려 사람이 조치해야 하는 사유(계속 나는 503·409 같은)는 ERROR 로 남긴다(ADR-011). 그 표시는 예외 타입이 든다(ADR-006, 메시지 문구로 가르지 않는다). 이 예시는 기본값만 보인다.
- `NullPointerException` 과 값 객체 생성자의 `IllegalArgumentException` 은 따로 매핑하지 않아 500 고정 문구다.
- 인증 필터의 401 도 같은 본문으로 낸다(ADR-019 예시의 `ErrorResponseWriter`).
- `traceId` 는 Micrometer Tracing 이 MDC 에 넣는 `traceId` 를 읽는다. 다른 추적 도구면 그 도구가 로그에 찍는 키를 쓴다.

## 기계 검사

리뷰가 체크리스트로 본다. [`checks/`](../../checks/README.md) 견본에는 이 ADR 을 막는 규칙이 없다.

공개 참고는 두지 않는다. 공개 예제 대부분이 `ProblemDetail`(RFC 9457) 본문이라 이 킷의 공통 응답 본문과 모양이 다르다.
