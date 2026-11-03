# ADR-006 예시: 오류는 이름이 사유를 말하는 예외로, 바깥 예외는 어댑터에서 감싼다

## 무엇을 보여 주나

주문 결제 표시와 영수증 저장에서 나는 오류다. 바른 모양은 도메인이 사유 예외를 던지고, 그 예외가 상태 공통 형제(`ConflictException`, `UnavailableException`)를 상속하며 도메인 ErrorCode 를 들고, 파일 I/O 예외는 영속 어댑터에서 `cause` 를 단 사유 예외로 바뀐다.
어긋난 모양은 범용 예외, 도메인 부모 예외와 예외마다의 code 상수, 로그만 남기고 삼키는 `catch` 다.

## 바른 모양

- `good/OrderErrorCode.java` - 주문 도메인의 오류 코드 enum. `getCode()` 는 `name()`, `getMessage()` 는 사용자 문구
- `good/OrderAlreadyPaidException.java` - `ConflictException`(409) 하나만 상속하고 `ORDER_ALREADY_PAID` 를 든다. 로그용 내부 문구는 `MESSAGE` 상수에 key=value 로
- `good/Order.java` - 이미 결제된 주문의 `markPaid` 가 사유 예외를 던진다. 생략한 compact 생성자의 `IllegalArgumentException` 은 불변 조건(프로그래밍 오류) 방어용이라 범용 예외여도 된다
- `good/FileOrderReceiptStorage.java` - `IOException` 을 `OrderReceiptStorageUnavailableException(orderId, e)` 로 감싼다. 서비스까지 I/O 예외가 올라가지 않는다

`OrderReceiptStorageUnavailableException` 은 `OrderAlreadyPaidException` 과 같은 모양에 부모가 `UnavailableException`(503)이고 `cause` 를 넘긴다. 공통 부모 `BusinessException`, 공통 형제, `CustomErrorCode` 는 `com.example.shared.exception` 에 있고 이름만 나온다.

## 어긋난 모양

- `bad/Order.java` - `throw new IllegalStateException("이미 결제된 주문: " + id)`. 사유가 문구에 숨고 경계는 500 으로 낸다
- `bad/OrderAlreadyPaidException.java` - 도메인 부모 `OrderException` 을 상속해 공통 형제를 못 타고, 예외마다 `CODE`, `USER_MESSAGE` 상수를 둔다
- `bad/FileOrderReceiptStorage.java` - `IOException` 을 WARN 로그만 남기고 삼킨다. 서비스는 영수증이 저장된 줄 안다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `throw new IllegalStateException("이미 결제된 주문: " + id)` | `throw new OrderAlreadyPaidException(id, status)` | 업무 사유에 `throw new RuntimeException/IllegalStateException/IllegalArgumentException` 이 없다(새로 생긴 것) |
| `extends OrderException` | `extends ConflictException` | 사유 예외가 공통 형제(어느 갈래에도 안 들면 `BusinessException`) 하나만 상속하고, 도메인 부모 예외가 없다 |
| 예외마다 `CODE`, `USER_MESSAGE` 상수 | `OrderErrorCode` enum, code 는 `name()` | 사유 예외가 자기 도메인 `{Domain}ErrorCode` 를 들고, enum 의 code 가 `name()` 이다(예외마다 `code()` 상수가 없다) |
| `catch (IOException e) { log.warn(...); }` | `catch (IOException e) { throw new OrderReceiptStorageUnavailableException(..., e); }` | 예외를 잡아 로그만 찍고 삼키는 `catch` 가 없다 / 외부 라이브러리 예외가 어댑터 밖으로 나가지 않는다(`cause` 를 단 사유 예외로) |

DB 에 적는 실패 사유(`코드: 메시지`)와 경계에서 HTTP 상태로 바꾸는 처리기는 이 예시에 없다. 처리기는 ADR-023 이 든다.

## 기계 검사

리뷰가 체크리스트로 본다. 범용 예외가 업무 사유인지 프로그래밍 오류인지는 기계로 가르지 못한다(`checks/README.md`).

## 공개 참고

- cheese10yun/spring-guide [`BusinessException`](https://github.com/cheese10yun/spring-guide/blob/master/src/main/java/com/spring/guide/global/error/exception/BusinessException.java) - 공통 부모, 공통 형제(`EntityNotFoundException`, `InvalidValueException`), 도메인별 `exception` 패키지 뼈대. 다른 점: 저장소 전체 `ErrorCode` enum 하나를 쓴다(이 ADR 은 도메인마다 enum), 2023 이후 갱신이 없다.
