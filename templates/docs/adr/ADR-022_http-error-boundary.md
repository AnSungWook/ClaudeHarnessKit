# ADR-022: HTTP 오류는 처리기 한 자리가 매핑 표 하나로 바꾸고, 컨트롤러는 바꾸고 부르고 바꾸기만 한다

## Status

Accepted

## Date

2026-11-03

## Intent

같은 사유가 어느 창구에서 나도 같은 HTTP 상태와 같은 본문으로 나가게 하고, 내부 메시지가 응답으로 새지 않게 하며, 컨트롤러를 읽으면 입구와 출구만 보이게 한다.

## Context

- 기존 상태: 컨트롤러마다 예외 처리기(`@RestControllerAdvice(assignableTypes = …)`)가 따로 있고, 어떤 것은 `ProblemDetail`, 어떤 것은 `Map` 을 낸다. 컨트롤러가 `ResponseStatusException` 을 직접 던진다. 요청 인자에 검증이 없다.
- 문제점: 같은 "없음" 이 창구마다 다른 상태와 다른 본문으로 나간다. 모르는 예외의 메시지(SQL, 클래스 이름, 경로)가 응답에 실린다. 검증과 판단이 컨트롤러에 흩어진다.
- 제약 조건: 클라이언트(화면, 스크립트, 다른 시스템)는 본문 모양에 기대므로 이미 공개한 창구의 본문은 함부로 못 바꾼다.

## Decision

**사유 예외를 HTTP 상태와 본문으로 바꾸는 일은 공통 처리기 한 자리가 아래 표 하나로 한다.**

- 오류 본문은 공통 응답 본문 하나다(예: `CommonResponse`, 오류는 `error{errorCode, errorMessage}`, 칸 검증은 `errors[]{field, reason}`). 성공 응답도 같은 본문으로 감싼다. `errorCode`, `errorMessage` 는 사유 예외가 든 ErrorCode 의 `getCode()`, `getMessage()` 다(ADR-005).
- 모듈은 자기 사유 예외를 공통 형제(ADR-005)에 붙여 매핑을 얻는다. 모듈마다 처리기를 따로 두지 않는다.
- 매핑은 이 표 하나다.

  | 예외 | 상태 | 본문 | 로그 |
  |---|---|---|---|
  | 요청 칸 검증(바인딩, 메서드 검증, 빠진 인자와 헤더, 못 읽는 본문, 타입이 안 맞는 값, 모르는 코드값), `InvalidValueException` | 400 | `errors[]`(칸, 사유) | WARN |
  | `IllegalArgumentException`(값 객체 생성자의 검증, ADR-001) | 400 | 고정 code `INVALID_VALUE` 와 고정 문구. 예외 문구는 싣지 않는다 | WARN(예외 문구) |
  | `ForbiddenException` | 403 | `error` | WARN |
  | `NotFoundException` | 404 | `error` | WARN |
  | `ConflictException`(중복과 상태 충돌) | 409 | `error` | WARN |
  | `UnavailableException`(바깥 시스템이나 저장소가 응하지 않음) | 503 | `error` | WARN |
  | 그 밖의 `BusinessException` | 400 | `error` | WARN |
  | 그 밖 전부 | 500 | 고정 code, 고정 문구 | ERROR(스택) |

- `NullPointerException` 은 따로 매핑하지 않아 500 이다. 필수 칸은 요청 검증이 먼저 400 으로 막으므로, 안쪽까지 온 null 은 코드의 버그다.
- 모르는 예외는 고정 code 와 고정 문구로만 응답한다. 원인과 스택은 로그에만 남긴다(ADR-012). 인증 필터의 401 도 같은 본문으로 낸다(ADR-018).
- 로그는 이 처리기에서 한 번 남긴다(ADR-010). 업무 4xx 와 503 은 WARN, 처리하지 못한 500 은 ERROR 다.
- 컨트롤러에서 `ResponseStatusException` 을 던지지 않는다. 사유 예외를 던지고 처리기가 바꾼다.
- 형식 검증은 요청 record 의 Bean Validation 과 인자의 `@Valid` 로 한다. `@RequestParam`, `@PathVariable` 에도 제약을 단다. 업무 검증은 서비스가 사유 예외로 한다.
  - 요청 record 의 필수 칸(코드값 enum 칸 포함)에는 `@NotNull` 이나 `@NotBlank` 를 달고, 선택 칸에는 달지 않는다.
  - Spring 6.1 이상이면 컨트롤러 클래스에 `@Validated` 를 달지 않는다. 메서드 검증이 내장이고, 달면 AOP 검증이 먼저 돌아 처리기가 받는 예외 타입이 갈린다.
- 컨트롤러 메서드는 요청을 바꾸고, 서비스를 한 번 부르고, 응답으로 바꾼다. 분기와 판단을 두지 않는다(ADR-001).
- 컨트롤러는 응답 DTO 를 낸다. 도메인 값이나 영속 엔티티를 그대로 내지 않는다(ADR-004).
- 목록은 페이지 번호와 크기를 받고 크기에 상한을 둔다. 새 경로는 kebab-case 복수 명사이고, 조건이 복잡한 조회는 `POST .../search` 다. 이미 공개한 경로와 본문은 계약이라 그대로 둔다.

### 체크리스트

- [ ] `@RestControllerAdvice`(또는 같은 일을 하는 처리기)가 저장소에 하나다
- [ ] 컨트롤러에 `ResponseStatusException` 이 없다
- [ ] 500 응답 본문에 예외 메시지, 클래스 이름, 스택이 없다(고정 문구)
- [ ] `IllegalArgumentException` 의 문구가 응답에 실리지 않는다
- [ ] 요청 record 의 필수 칸에 `@NotNull`·`@NotBlank`, `@RequestParam`·`@PathVariable` 에 제약이 있다
- [ ] 컨트롤러 메서드에 분기나 판단이 없다(바꾸고, 한 번 부르고, 바꾼다)
- [ ] 컨트롤러가 엔티티나 도메인 값을 그대로 내지 않는다
- [ ] 예외 로그가 처리기 한 자리에서만 남는다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| 컨트롤러마다 `@RestControllerAdvice(assignableTypes = XController.class)` | 공통 처리기 하나와 매핑 표 하나 |
| `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order " + id)` | `throw new OrderNotFoundException(id)`(→ `NotFoundException` → 404) |
| 500 본문에 `e.getMessage()` | 고정 code·고정 문구, 원인은 ERROR 로그 |
| 컨트롤러에서 `if (req.amount() <= 0) return badRequest()` | 요청 record 의 `@Positive`, 업무 규칙은 서비스의 사유 예외 |
| 서비스에서 잡아 `log.error` 하고 다시 던지고, 처리기에서 또 `log.error` | 처리기 한 자리에서 한 번 |
| 엔티티를 그대로 JSON 으로 | 응답 DTO 로 바꿔 낸다 |

## 다른 언어에서

- 원칙(처리기 한 자리, 매핑 표 하나, 모르는 예외는 고정 문구, 컨트롤러는 얇게)은 같다.
- Kotlin: Spring 이면 그대로. Ktor 는 `StatusPages` 플러그인 한 자리에서 매핑한다.
- TypeScript(NestJS): 전역 `ExceptionFilter` 하나에 매핑하고, 요청 검증은 `ValidationPipe` 와 DTO 클래스의 데코레이터로. Express 는 마지막 오류 미들웨어 하나.
- Python(FastAPI): `app.exception_handler` 한 자리에 매핑하고, 요청 검증은 Pydantic 모델로. Django REST framework 는 `EXCEPTION_HANDLER` 하나.

## Consequences

### 긍정적
- 같은 사유가 어느 창구에서도 같은 상태와 본문으로 나간다.
- 내부 메시지가 응답으로 새지 않는다.
- 컨트롤러가 짧아져 입구와 출구만 읽힌다.

### 부정적
- 이미 다른 본문을 쓰는 창구를 옮기면 클라이언트가 같이 바뀌어야 한다 - 그런 창구는 경로 접두로 옛 본문을 유지하는 예외를 처리기에 둘 수 있다.
- 매핑 표에 없는 새 갈래가 생기면 표와 공통 형제를 같이 고쳐야 한다.

## References

- ADR-001(값 객체 검증), ADR-004(바깥 계약과 도메인 분리), ADR-005(사유 예외, 공통 형제, ErrorCode), ADR-010(로그), ADR-012(입력 검증과 내부 정보), ADR-018(인증 경계)
