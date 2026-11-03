# ADR-005 예시: 층은 역할로, 의존은 안쪽으로만

## 무엇을 보여 주나

주문 한 건을 읽어 응답하는 흐름이다. 바른 모양은 컨트롤러(어댑터) → 서비스 → 포트 ← 영속 어댑터로, 서비스는 포트와 도메인만 알고 엔티티는 영속 어댑터 밖으로 나가지 않는다.
어긋난 모양은 서비스가 Spring Data 저장소와 엔티티를 바로 쓰고, 컨트롤러가 엔티티를 응답으로 낸다.

## 바른 모양

- `good/OrderController.java` - `adapter/in/web`. 요청 → 서비스 한 번 → `OrderResponse.of(domain)` 응답. 같은 기능의 입구가 하나라 `port/in` 없이 서비스를 바로 부른다(ADR-017 이 허용하는 줄임)
- `good/OrderQueryService.java` - `application/service`. 포트 `OrderRepository` 만 안다. 없으면 사유 예외(`get`)
- `good/OrderRepository.java` - `application/port/out`. 도메인 `Order` 로 말하는 약속. 읽기 `find`(Optional), 쓰기 `save`
- `good/JpaOrderRepository.java` - `adapter/out/persistence`. Spring Data 저장소(`OrderJpaRepository`)와 엔티티(`OrderEntity`)를 감추고 도메인으로 번역한다. 엔티티 → 도메인은 엔티티의 `toOrder()`, 도메인 → 엔티티는 `OrderEntity.of(order)` 로 어댑터 안에서 끝난다

`OrderResponse`, `OrderEntity`, `OrderJpaRepository`, `CommonResponse`(ADR-023 의 공통 응답 본문)는 이름만 나온다.

## 어긋난 모양

- `bad/OrderQueryService.java` - 서비스가 `adapter.out.persistence` 의 `OrderJpaRepository`, `OrderEntity` 를 import 하고 엔티티를 돌려준다
- `bad/OrderController.java` - 그 엔티티를 응답 본문으로 그대로 낸다. 엔티티 칸 하나를 바꾸면 API 계약이 같이 바뀐다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| 서비스가 `OrderJpaRepository` 를 주입받음 | 서비스는 포트 `OrderRepository` 만 | 서비스가 어댑터 구현 클래스를 직접 참조하지 않는다(포트만) |
| 서비스가 `OrderEntity` 를 돌려줌 | 영속 어댑터가 `toOrder()` 로 번역해 도메인 `Order` 를 돌려줌 | 외부 응답 타입이 어댑터 밖으로 나가지 않는다(도메인 값으로 번역) |
| 컨트롤러가 `CommonResponse<OrderEntity>` | `CommonResponse<OrderResponse>`, `OrderResponse.of(order)` | 컨트롤러가 엔티티를 그대로 응답하지 않는다(응답 타입의 `of(domain)`) |
| (변환 Mapper 층) | 만들어지는 쪽이 원본을 아는 `of(...)` / 엔티티의 도메인 변환 `to...` | 도메인이 요청·응답 DTO 나 엔티티 타입을 import 하지 않는다 |

## 기계 검사

- `checks/AdrArchitectureTest.java` 의 `dependenciesPointInward` 가 `application` → `adapter` 의존을 막는다. `bad/OrderQueryService.java` 의 import 가 여기에 걸린다.
- `bad/OrderController.java` 처럼 어댑터 층 안에서 다른 어댑터의 엔티티를 쓰는 것, 도메인의 바깥 라이브러리 참조는 이 규칙이 안 본다 - 리뷰가 체크리스트로 본다.

## 공개 참고

- buckpal [`SendMoneyService`](https://github.com/thombergs/buckpal/blob/master/src/main/java/io/reflectoring/buckpal/application/domain/service/SendMoneyService.java) 와 [`DependencyRuleTests`](https://github.com/thombergs/buckpal/blob/master/src/test/java/io/reflectoring/buckpal/DependencyRuleTests.java) - 포트·어댑터 배치와 ArchUnit 의존 검사. 다른 점: 변환을 `AccountMapper` 클래스에 모은다(ADR-002 는 `of(원본)`), 서비스 안에 판단이 있다(ADR-002, ADR-005 는 서비스가 흐름만), `LocalDateTime.now()` 를 부른다(ADR-003), 2024 이후 갱신이 없다.
