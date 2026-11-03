# ADR-021 예시: 같은 일은 짧고 선언적인 최신 문법 한 모양으로

[ADR-021](../../ADR-021_modern-language-idioms.md) 을 결제 결과 반영과 주문 줄 집계에 적용한 예다. Java 21 기준.

## 무엇을 보여 주나

결제사 결과(승인, 거절)를 sealed 타입으로 받아 `switch` 식과 record 패턴으로 가르고, 주문 줄 목록을 스트림으로 걸러 모으고 묶는다.
새 결과 갈래나 새 거절 사유를 빠뜨리면 컴파일이 깨지는 모양이다.

## 바른 모양

- [`good/PaymentOutcome.java`](good/PaymentOutcome.java) - 닫힌 결과 집합 `sealed interface` 와 record 갈래 `Approved`, `Declined`. 갈래 중첩은 ADR-004 의 허용 예다.
- [`good/Payment.java`](good/Payment.java) - `switch` 식에 타입 패턴, record 패턴, `when` 가드. `default` 없이 갈래를 다 나열한다.
- [`good/PaymentDeclineReason.java`](good/PaymentDeclineReason.java) - enum 에 따른 판단(`isRetryable`)을 enum 안에, `default` 없는 `switch` 식으로.
- [`good/OrderLines.java`](good/OrderLines.java) - 결과가 나오는 순회는 스트림(`filter`, `toList()`, `groupingBy`, `joining`). 돌려주는 맵도 불변(`Map::copyOf`, ADR-009).

## 어긋난 모양

- [`bad/Payment.java`](bad/Payment.java) - `instanceof` 뒤 캐스팅, 문장형 `switch` 와 `default`, 마지막 `return` 이 나머지 갈래를 모두 승인으로 받는다.
- [`bad/OrderLines.java`](bad/OrderLines.java) - `for` + `new ArrayList<>()`, 스트림 `forEach` 로 바깥 맵 고치기, 문자열 `+` 잇기.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `List<OrderLine> result = new ArrayList<>(); for (...) result.add(...)` | `lines.stream().filter(OrderLine::isCouponApplicable).toList()` | 결과를 쌓아 돌려주는 `for` + `new ArrayList<>()` 가 새로 들지 않았다 |
| `lines.stream().forEach(line -> amounts.merge(...))` | `collect(groupingBy(..., summingLong(...)))` | 스트림 `forEach` 안에서 바깥 컬렉션을 고치지 않는다 |
| `switch (reason) { case GATEWAY_TIMEOUT: return ...; default: ... }` | `PaymentDeclineReason.isRetryable()` 의 `switch` 식 | 문장형 `switch` 가 새로 들지 않았다 / enum `switch` 에 `default` 가 없다 |
| `if (outcome instanceof Declined) { Declined d = (Declined) outcome; ... }` | `case PaymentOutcome.Declined(PaymentDeclineReason reason) when ...` | `instanceof` 뒤 별도 캐스팅이 없다 |
| 마지막 `return withStatus(APPROVED)` 가 남은 갈래를 받음 | sealed 갈래를 `default` 없이 전부 나열 | sealed 타입 `switch` 에 `default` 가 없다 |
| `names = names + ", " + ...` | `Collectors.joining(PRODUCT_NAME_DELIMITER)` | (Decision: 잇기는 `String.join`, `Collectors.joining`) |

- `var` 는 오른쪽에서 타입이 바로 보일 때만 쓴다(`var lines = new OrderLines(items);` 는 되고 `var result = gateway.approvePayment(payment);` 는 안 된다). 이 예시 코드는 `var` 를 쓰지 않았다.
- `Approved approved`, `Declined declined` 처럼 쓰지 않는 바인딩 이름은 Java 21 에서 뺄 수 없다. Java 22 이상이면 `_` 로 쓴다.

## 기계 검사

리뷰가 체크리스트로 본다. [`checks/`](../../checks/README.md) 견본에는 이 ADR 을 막는 규칙이 없다.

## 공개 참고

- nipafx/java-after-eight `upgrades` 브랜치 [`TypeGenealogist`](https://github.com/nipafx/java-after-eight/blob/upgrades/genealogists/src/main/java/org/codefx/java_after_eight/genealogists/type/TypeGenealogist.java) - sealed 타입과 `default` 없는 `switch` 식 패턴. `main` 브랜치의 같은 파일이 옛 모양이라 전후를 견줄 수 있다.
  - 우리 ADR 과 다른 점: 2022 뒤로 갱신이 멈췄다.
