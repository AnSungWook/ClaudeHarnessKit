# ADR-011 예시: 로그는 수준을 뜻대로, 한 번만, 안전하게

[ADR-011](../../ADR-011_logging.md) 을 주문 생성, 결제사 호출, 공통 예외 처리기에 적용한 예다.

## 무엇을 보여 주나

업무 사건은 INFO 에 식별자만 `key={}` 꼴로 남기고, 예외는 중간에서 찍지 않고 감싸 던져 경계 처리기 한 자리에서 한 번만 남긴다.
수준은 뜻으로 고른다 - 요청자가 고칠 일은 WARN, 처리하지 못한 예외는 ERROR 와 스택.
바깥 API·DB 예외의 문구와 원인 사슬에는 카드번호, 토큰, SQL 바인딩 값이 실릴 수 있어, 처리기는 원본 대신 문구를 버린 사슬(`LogSafeException`)을 넘긴다.

## 바른 모양

- `good/OrderPlacementService.java` - 주문 생성을 INFO 한 줄로. 요청을 통째로 찍지 않고 `orderId`, `memberId`, `totalAmount` 만. 예외를 잡지 않는다.
- `good/PaymentGatewayAdapter.java` - 라이브러리 예외를 사유 예외로 감싸(`cause` 를 달고) 던지기만 한다. 여기서 로그를 남기지 않는다. 응답 본문이 비면 NPE 대신 사유 예외로 낸다(ADR-013, 바깥 값도 입력).
- `good/GlobalExceptionHandler.java` - 예외 로그를 남기는 한 자리. 요청 검증 실패와 업무 사유 예외는 WARN, 모르는 예외는 ERROR. 스택은 `LogSafeException.of(e)` 로 넘겨 위치만 남고 문구는 빠진다. 상태는 ADR-023 매핑 표의 갈래(400, 403, 404, 409, 503, 그 밖 업무 400, 그 밖 500)를 빠짐없이 따른다 - 업무 예외가 500 이 되지 않는다. 본문(`errors[]`, `traceId`)을 만드는 모양은 ADR-023 예시가 든다.
- `good/LogSafeException.java` - 공통 도우미 `com.example.shared.logging.LogSafeException`. `of(cause)` 가 원인 사슬을 따라가며 단계마다 클래스 이름만 담고 스택 위치(`setStackTrace`)는 그대로인 사슬을 만든다. 던지지 않고 로그에만 넘긴다.

## 어긋난 모양

- `bad/OrderPlacementService.java` - `System.out`, 문자열 `+` 연결, 요청 통째로, 업무 사건을 ERROR 로, 찍고 다시 던지기.
- `bad/PaymentGatewayAdapter.java` - `printStackTrace()`, 카드번호가 든 요청 본문을 로그에, 어댑터에서 찍고 감싸 던져 경계에서 한 번 더 찍힌다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `System.out.println(...)`, `e.printStackTrace()` | `log.info(...)`, 경계의 `log.error(..., e)` | `System.out`, `printStackTrace` 가 없다 |
| `log.info("placeOrder request=" + command)` | `log.info("Order placed: orderId={}, ...", ...)` | 로그 메시지에 문자열 `+` 연결이 없다 |
| 서비스에서 `log.error(...); throw e;`, 어댑터에서 `log.error(...)` 뒤 감싸 던지기 | 감싸 던지기만, 로그는 `GlobalExceptionHandler` 한 자리 | 같은 예외를 잡아 로그를 찍고 다시 던지는 자리가 없다 |
| `request={}` 에 카드번호가 든 본문, `command` 통째로 | 식별자(`orderId`, `paymentId`)만 | 토큰·비밀번호·개인정보·본문 전체를 로그에 넣지 않는다 |
| 어댑터에서 `log.error(..., e)` 로 바깥 예외를 문구째 | 처리기에서 `LogSafeException.of(e)` - 클래스 이름과 스택 위치만 | 바깥 API·DB 예외를 남길 때 문구와 원인 체인의 민감값을 가리거나 뺐다 |
| 주문 생성을 `log.error` | INFO | ERROR 는 조치가 필요한 경우에만 쓴다 |

- `PaymentGateway`, `PaymentApproval`, `PaymentGatewayApproveRequest`/`Response`, `PaymentGatewayEmptyResponseException`, `CommonResponse`, 공통 부모와 형제 예외(`BusinessException`, `NotFoundException`, `ConflictException` 등, ADR-006)는 이름으로만 쓴다.
- 결제사 클라이언트 빈의 타임아웃과 재시도 없음(승인은 멱등이 아니다)은 ADR-018 이 정하고, 이 예시는 다루지 않는다.

## 기계 검사

- [`checks/AdrArchitectureTest.java`](../../checks/README.md) 가 `System.out`, `System.err`, `printStackTrace` 를 막는다.
- 문자열 연결, 찍고 다시 던지기, 민감값, 수준은 리뷰가 체크리스트로 본다.
