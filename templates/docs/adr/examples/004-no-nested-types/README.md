# ADR-004 예시: 중첩 타입은 업무가 실제로 겹칠 때만

## 무엇을 보여 주나

주문과 주문 줄, 그리고 결제 결과 갈래다. 주문 줄은 주문 밖에서도 쓰이는 독립 값이라 최상위 타입 `OrderLine` 으로 꺼내고, 결제 결과의 갈래(`Approved`, `Declined`)는 부모 없이는 뜻이 없어 중첩을 남긴다.
설정 키 깊이를 따른 설정 record 의 중첩은 [ADR-003 예시](../003-constants-and-settings/README.md)의 `OrderProperties` 가 보인다.

## 바른 모양

- `good/OrderLine.java` - 같은 패키지의 최상위 record. 자기 판단(`subtotal`, `isBulk`)과 불변 조건 검사를 자기 파일에 든다
- `good/Order.java` - `List<OrderLine>` 을 들고 주문 단위 판단만 한다
- `good/PaymentOutcome.java` - sealed 부모와 갈래 record. 세 조건(부모 없이 뜻이 없다, 같이 읽어야 한다, 따로 안 쓰인다)을 다 만족하는 허용 예이고, 남긴 이유를 선언 위 주석과 아키텍처 테스트 허용 목록에 적는다

## 어긋난 모양

- `bad/Order.java` - 주문 줄을 `Order.Line` 으로 중첩했다. 장바구니나 정산에서 쓰려면 `Order.Line` 을 달고 다녀야 하고, 줄의 판단 메서드가 `Order` 파일을 키운다

`private` 으로 감춘 중첩(`class OrderPlacementService { private record Line(...) {} }`)도 같다. 가시성이 아니라 업무 관계로 판단한다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `Order.Line` 중첩 record | 같은 패키지의 `OrderLine` | 새 중첩 타입이 위 세 조건을 다 만족한다(설정 바인딩 record 의 중첩은 허용 예) |
| 줄의 `subtotal`, `isBulk` 가 `Order` 파일 안 | `OrderLine` 파일 안 | 꺼낸 타입의 이름이 바깥 이름 없이도 뜻이 드러난다 |
| (sealed 갈래까지 전부 떼어 파일을 흩뿌림) | `PaymentOutcome.Approved`, `PaymentOutcome.Declined` 중첩 + 이유 주석 | 허용 목록에 이유 없는 중첩 타입이 없다 |

`Order.Line` 을 `OrderLine` 으로 꺼내도 JSON 칸 이름(`lines[].productCode` 등)은 그대로다. 직렬화 모양을 바꾸지 않는다.

## 기계 검사

리뷰가 체크리스트로 본다. `checks/AdrArchitectureTest.java` 에는 중첩 타입 규칙이 없다. 프로젝트가 규칙을 더하면 `PaymentOutcome.Approved - 갈래는 부모와 함께 switch 로 읽힌다` 처럼 허용 목록에 이유를 함께 적는다.

## 공개 참고

- spring-data-commons [`Limit`](https://github.com/spring-projects/spring-data-commons/blob/main/src/main/java/org/springframework/data/domain/Limit.java) - sealed 부모와 그 갈래(`Limited`, `Unlimited`)를 중첩한 모양. 다른 점: 업무 코드가 아니라 라이브러리 코드다.
