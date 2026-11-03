# ADR-005: 오류는 도메인별 사유 예외로 던진다 - 클래스 이름이 사유를 말한다

## Status

Accepted

## Date

2026-10-30

## Intent

스택 첫 줄만 보고 무엇 때문에 실패했는지 알게 하고, 경계에서 사유별로 응답 코드·재처리 여부를 쉽게 가르게 한다.

## Context

- 기존 상태: 업무 사유에 `IllegalStateException("이미 실행 중")`, `RuntimeException("규칙 없음")` 처럼 범용 예외와 메시지를 쓴다. 외부 라이브러리 예외가 서비스까지 그대로 올라온다.
- 문제점: 사유가 메시지 문구 속에 숨어 로그를 읽어야 원인을 안다. 문구로 분기하면 문구 하나 바꿀 때 처리가 깨진다.
- 제약 조건: 체크 예외는 시그니처를 타고 퍼지고 람다에서 쓰기 불편하다.

## Decision

**업무 오류는 `RuntimeException` 을 상속한, 이름이 사유를 말하는 도메인별 예외로 던진다.**

- 체크 예외를 새로 만들지 않는다.
- 이름은 "무엇이 + 어떻게": `OrderAlreadyPaidException`, `UnknownCouponException`, `UploadRejectedException`. 같은 도메인의 사유가 여럿이면 부모 하나에 갈래를 둔다(`UploadRejectedException` ← `UploadMissingException`).
- 업무 사유에 범용 예외(`RuntimeException`, `IllegalStateException`, `IllegalArgumentException`)를 직접 던지지 않는다. 범용 예외는 "있을 수 없는 상태"(프로그래밍 오류)와 값 객체 생성자의 인자 검증에만.
- 외부 라이브러리 예외(I/O, HTTP 클라이언트, 드라이버 예외)는 어댑터에서 사유 예외로 감싼다. 원인(`cause`)을 달고, 서비스까지 라이브러리 예외가 올라오지 않게 한다(ADR-004).
- 예외는 그 도메인 패키지의 `exception/` 에 둔다. 재시도 여부 같은 처리 표시는 예외 타입이 든다(메시지 문구로 판단하지 않는다).
- 위계는 공통 부모 `BusinessException` 하나 아래에 둔다. 도메인(모듈)마다 부모를 하나 두고(`OrderException`, `PaymentException`) 그 도메인의 사유 예외가 상속한다.
  - 경계가 HTTP 상태로 바꾸는 공통 형제 다섯을 같은 공통 패키지에 둔다: 없음 `NotFoundException`(404), 충돌 `ConflictException`(409, 중복과 상태 충돌), 금지 `ForbiddenException`(403), 잘못된 값 `InvalidValueException`(400, 칸 이름과 사유를 든 `errors[]`), 응답 없음 `UnavailableException`(503, 바깥 시스템이나 저장소가 응하지 않음). 도메인 사유 가운데 이 갈래인 것만 그 형제를 상속한다(ADR-022).
  - 업무 사유가 아니라 바깥 호출의 기술 실패만 다루는 모듈(외부 모델, 결제사 게이트웨이 같은)은 `BusinessException` 밖에 따로 위계를 둘 수 있다. 그때도 sealed 부모 하나와 그 모듈의 오류 코드 enum 을 든다.
- **오류 코드는 도메인마다 enum `{Domain}ErrorCode implements CustomErrorCode` 로 둔다.** `CustomErrorCode` 는 `getCode()` 와 `getMessage()` 두 메서드다.
  - enum 에 code 칸을 따로 두지 않는다. `getCode()` 가 `name()` 을 돌려줘 code 와 enum 이름이 어긋날 길이 없다. `getMessage()` 는 사용자에게 보이는 문구다.
  - 사유 예외는 자기 ErrorCode 를 든다. 예외마다 `code()` 를 따로 정의하지 않는다. 경계 처리기가 그 code 와 message 로 오류 본문을 만든다(ADR-022).
  - 예외 안 문구는 둘이고 섞지 않는다. 로그에 남는 내부 문구는 예외 클래스의 `MESSAGE` 상수에 key=value 를 덧붙여 만들고(ADR-010), 사용자 문구는 ErrorCode 의 message 다.
  - 공개한 code 는 이름을 바꾸지 않는다. 바꿔야 하면 옛 상수를 `@Deprecated` 로 두고 새 code 를 더한다.

  ```java
  public interface CustomErrorCode {
      String getCode();
      String getMessage();
  }

  @Getter
  @RequiredArgsConstructor
  public enum OrderErrorCode implements CustomErrorCode {
      ORDER_ALREADY_PAID("이미 결제된 주문입니다."),
      ORDER_NOT_FOUND("주문을 찾을 수 없습니다.");

      private final String message;

      @Override
      public String getCode() {
          return name();
      }
  }

  public class OrderAlreadyPaidException extends OrderException {
      private static final String MESSAGE = "Order already paid: orderId=%d, status=%s";

      public OrderAlreadyPaidException(long orderId, String status) {
          super(OrderErrorCode.ORDER_ALREADY_PAID, MESSAGE.formatted(orderId, status));
      }
  }
  ```
- **DB 에 적는 실패 사유는 `코드: 메시지` 모양이다.** 예외 클래스 이름을 적지 않는다 - 클래스 이름은 리팩토링으로 바뀌지만 code 는 공개한 뒤 바꾸지 않는다. 사유 글자로 재시도 횟수나 처리 갈래를 가르지 않는다(가를 일이 있으면 상태 칸을 둔다).
  - 메시지 자리는 예외의 내부 문구(값 포함)다. 사용자 문구가 아니다 - 사용자 문구는 고정이라 무엇이 왜 실패했는지 값이 빠진다.
  - ErrorCode 를 들지 않은 그 밖의 예외(범용, 프레임워크 예외)는 문구 없이 `INTERNAL_ERROR: 예외 클래스 전체 이름` 만 적는다(예: `INTERNAL_ERROR: java.lang.NullPointerException`). 그런 문구에는 SQL 이나 개인정보가 섞일 수 있다. 문구와 스택은 로그로 간다.
  - 이 칸을 바깥 응답으로 내보낼 때는 클래스 이름과 문구를 빼고 고정 문구로 바꾼다(ADR-012, ADR-022).
- 잡는 자리는 경계뿐 - 컨트롤러 예외 처리기(HTTP 상태로), 배치 스텝, 작업 단위 하나(실패 결과로). 중간에서 잡아 삼키지 않는다.
- 실패를 숨기지 않는다. 잡았으면 사유 예외로 바꿔 다시 던지거나, 경계에서 실패 결과(상태·사유)로 남긴다. 다음은 금지:
  - 빈 `catch`, `catch (Exception e)` 로 넓게 잡고 넘어가기.
  - 로그만 남기고 성공처럼 계속 가기 - 배치에서 한 건의 실패를 넘기고 다음 건으로 가는 것은 그 건을 실패 결과로 남길 때만 된다.
  - 실패를 기본값·빈 목록·`null` 로 바꿔 돌려주기(호출자가 실패를 모른다).
- 사용자 입력 오류는 경계에서 업무 사유 예외로 낸다. 값 객체 생성자의 검사(ADR-001)는 프로그래밍 오류 방어용이다.

### 체크리스트

- [ ] 업무 사유에 `throw new RuntimeException/IllegalStateException/IllegalArgumentException` 이 없다(새로 생긴 것)
- [ ] 새 예외 이름만 보고 사유를 알 수 있다
- [ ] 사유 예외가 자기 도메인 `{Domain}ErrorCode` 를 들고, enum 의 code 가 `name()` 이다(예외마다 `code()` 상수가 없다)
- [ ] 공개한 code 이름을 바꾸지 않았다(바꿀 때는 `@Deprecated` 와 새 상수)
- [ ] DB 실패 사유가 `코드: 메시지` 이고, ErrorCode 없는 예외는 `INTERNAL_ERROR: 클래스 이름` 만 남는다
- [ ] 외부 라이브러리 예외가 어댑터 밖으로 나가지 않는다(`cause` 를 단 사유 예외로)
- [ ] 예외를 잡아 로그만 찍고 삼키는 `catch` 가 없다
- [ ] 메시지 문구로 분기하는 코드가 없다
- [ ] 실패를 기본값·빈 목록·`null` 로 바꿔 돌려주는 `catch` 가 없다
- [ ] 배치에서 넘긴 실패는 그 건의 실패 결과로 남는다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `throw new IllegalStateException("이미 결제됨")` | `throw new OrderAlreadyPaidException(orderId)` |
| `catch (IOException e) { throw new RuntimeException(e); }` 를 서비스에서 | 어댑터에서 `throw new ReportStorageUnavailableException(key, e)` |
| `if (e.getMessage().contains("timeout"))` 로 재시도 판단 | 예외 타입(또는 타입이 든 재시도 표시)으로 판단 |
| 에러 코드 enum 하나 + 범용 예외 하나 | 사유마다 이름 있는 예외 클래스 + 그 예외가 드는 도메인 ErrorCode |
| 저장소 전체 에러 코드 enum 하나 | 도메인마다 enum - 모든 모듈이 한 파일을 고치지 않게 |
| 예외마다 `CODE`, `USER_MESSAGE` 상수 | 도메인 ErrorCode enum 한 자리, code 는 `name()` |
| 실패 사유 칸에 `com.example.order.OrderAlreadyPaidException: ...` | `ORDER_ALREADY_PAID: Order already paid: orderId=1, status=PAID` |
| 실패 사유 칸에 `NullPointerException` 의 문구 | `INTERNAL_ERROR: java.lang.NullPointerException`, 문구와 스택은 로그 |
| 체크 예외 `throws OrderException` 를 계층마다 전파 | 언체크 사유 예외, 경계에서만 잡기 |
| 중간 계층에서 `catch (Exception e) { log.warn(...) }` | 경계까지 올려 거기서 응답·실패 결과로 |

## 다른 언어에서

- Kotlin: Java 와 같다(체크 예외가 없으니 사유 예외 클래스만).
- 오류 코드 enum 은 언어의 enum 이나 문자열 리터럴 유니온으로 같은 모양(도메인마다 하나, 이름이 곧 code)을 둔다.
- TypeScript: `Error` 를 상속한 사유 클래스(`class OrderAlreadyPaidError extends Error`). 예상되는 실패가 많은 흐름은 결과 타입(`Result`)을 써도 되지만, 그때도 실패 종류는 타입으로 구분한다.
- Python: `Exception` 을 상속한 사유 예외. 맨 `except:`, `except Exception: pass` 금지는 같다.

## Consequences

### 긍정적
- 스택 첫 줄에서 사유가 보인다.
- 경계에서 사유별 HTTP 상태·재처리 여부를 타입으로 가른다.
- 메시지 문구를 바꿔도 처리가 안 깨진다.

### 부정적
- 예외 클래스 수가 늘어난다.
- 어댑터마다 외부 예외를 감싸는 코드가 필요하다.

## References

- ADR-004 헥사고날 층과 의존 방향
