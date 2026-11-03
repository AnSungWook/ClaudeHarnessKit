# ADR-002 예시: 판단이 드는 값은 그 타입의 정적 팩토리가 만든다

## 무엇을 보여 주나

주문의 결제 결과(`OrderPaymentResult`)를 정하는 자리다. 승인이면 PAID, 아니면 PENDING 과 대기 사유를 남긴다.
바른 모양은 그 판단을 결과 타입의 `of(원본...)` 이 다 들고 서비스는 흐름만 남는다. 어긋난 모양은 서비스가 생성 인자 안에서 상태와 문구를 정한다.

## 바른 모양

- `good/OrderPaymentResult.java` - `of(order, approval, clock)` 가 상태, 대기 사유, 정한 시각까지 다 정한다. 생성자(compact)는 "PAID 면 사유 없음, PENDING 이면 사유 있음" 불변 조건을 검사해 잘못된 값이 만들어지지 않게 한다
- `good/OrderPaymentService.java` - 읽고, `OrderPaymentResult.of(...)` 를 부르고, 저장한다. 판단은 한 줄도 없다

판단 없는 값(`PaymentApproval` 같은, 어댑터가 바깥 응답을 옮겨 담는 값)은 팩토리 없이 생성자 `new` 로 만든다. 이 예시에는 이름만 나온다.

## 어긋난 모양

- `bad/OrderPaymentService.java` - `approval.isApproved() ? PAID : PENDING`, `"결제 대기: " + ...` 를 생성 인자 안에서 정한다. 같은 조립이 다른 서비스에 또 생기면 두 자리가 따로 바뀐다
- `bad/OrderPaymentResult.java` - `of(...)` 가 매개변수를 그대로 생성자에 넘기는 껍데기다. 불변 조건 검사도 없어 PAID 인데 대기 사유가 든 값이 만들어진다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| 서비스가 `approval.isApproved() ? PAID : PENDING` 을 생성 인자로 | `OrderPaymentResult.of(order, approval, clock)` 안에서 | 생성되는 타입 밖에서 생성 인자에 `? :`, 문자열 `+`, 계산이 없다 |
| 서비스가 `"결제 대기: " + reason` 조립 | 팩토리 안의 `"결제 대기: %s".formatted(...)` | 상태·기본값·실패 문구 같은 판단이 서비스가 아니라 그 값의 타입 안에 있다 |
| `static of(a, b, c, d) { return new X(a, b, c, d); }` | 판단이 있는 `of(원본...)`, 판단 없는 값은 생성자 | 본문이 `return new 자기타입(인자 그대로)` 뿐인 팩토리가 없다 |
| 불변 조건 검사 없는 record | compact 생성자가 상태와 사유의 짝을 검사 | 값 객체 생성자(또는 팩토리)가 불변 조건을 검사한다 |

생성자의 `IllegalArgumentException` 은 프로그래밍 오류 방어선이다. 사용자 입력 오류는 요청 검증과 사유 예외(ADR-006, ADR-013)가 먼저 막는다.

## 기계 검사

리뷰가 체크리스트로 본다. `checks/` 에는 이 ADR 을 막는 규칙이 없다(생성 인자 안의 판단은 바이트코드로 가르기 어렵다).

## 공개 참고

- ddd-by-examples/library [`HoldDuration`](https://github.com/ddd-by-examples/library/blob/master/src/main/java/io/pillopl/library/lending/patron/model/HoldDuration.java) - 갈래 이름 팩토리(`openEnded`, `closeEnded`)와 private 생성자 검사. 다른 점: 인자 없는 갈래가 `Instant.now()` 를 부른다(ADR-003 은 주입한 `Clock`), Lombok `@Value` 를 쓴다.
