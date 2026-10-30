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
- 잡는 자리는 경계뿐 - 컨트롤러 예외 처리기(HTTP 상태로), 배치 스텝, 작업 단위 하나(실패 결과로). 중간에서 잡아 삼키지 않는다.
- 실패를 숨기지 않는다. 잡았으면 사유 예외로 바꿔 다시 던지거나, 경계에서 실패 결과(상태·사유)로 남긴다. 다음은 금지:
  - 빈 `catch`, `catch (Exception e)` 로 넓게 잡고 넘어가기.
  - 로그만 남기고 성공처럼 계속 가기 - 배치에서 한 건의 실패를 넘기고 다음 건으로 가는 것은 그 건을 실패 결과로 남길 때만 된다.
  - 실패를 기본값·빈 목록·`null` 로 바꿔 돌려주기(호출자가 실패를 모른다).
- 사용자 입력 오류는 경계에서 업무 사유 예외로 낸다. 값 객체 생성자의 검사(ADR-001)는 프로그래밍 오류 방어용이다.

### 체크리스트

- [ ] 업무 사유에 `throw new RuntimeException/IllegalStateException/IllegalArgumentException` 이 없다(새로 생긴 것)
- [ ] 새 예외 이름만 보고 사유를 알 수 있다
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
| 에러 코드 enum 하나 + 범용 예외 하나 | 사유마다 이름 있는 예외 클래스 |
| 체크 예외 `throws OrderException` 를 계층마다 전파 | 언체크 사유 예외, 경계에서만 잡기 |
| 중간 계층에서 `catch (Exception e) { log.warn(...) }` | 경계까지 올려 거기서 응답·실패 결과로 |

## 다른 언어에서

- Kotlin: Java 와 같다(체크 예외가 없으니 사유 예외 클래스만).
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
