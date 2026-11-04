# ADR-017 예시: 패키지는 기능별로, 그 안은 성격별 폴더로 고정한다

[ADR-017](../../ADR-017_package-structure.md) 의 기본 배치를 주문과 결제 두 기능에 적용한 예다.

## 무엇을 보여 주나

최상위는 기능(`order`, `payment`), 그 안은 `domain`, `application/port`, `application/service`, `adapter/in`, `adapter/out`, `config` 로 고정한 폴더 트리다.
결제가 주문을 볼 때 주문의 `port/in` 과 `domain` 공개 값만 쓰고, 영속 엔티티는 주문을 키 칸으로 든다.

## 바른 모양

- [`good/tree.md`](good/tree.md) - 기본 배치의 폴더 트리. 작은 기능(`payment`)은 쓰지 않는 폴더를 만들지 않는다.
- [`good/GetOrderSummaryUseCase.java`](good/GetOrderSummaryUseCase.java) - 주문이 다른 기능에 여는 입구(`order/application/port/in`). 여러 건은 키 목록으로 한 번에.
- [`good/PaymentCreationService.java`](good/PaymentCreationService.java) - 결제 서비스가 주문의 `port/in` 과 `domain.OrderSummary` 만 import 한다.
- [`good/PaymentJpaEntity.java`](good/PaymentJpaEntity.java) - 결제 영속 엔티티(`payment/adapter/out/persistence`). 주문은 `orderId` 키 칸으로 든다. 변환은 만들어지는 쪽이 원본을 아는 `of(payment)` 다(도메인 값으로 되돌리는 `toPayment()` 는 줄였다).

## 어긋난 모양

- [`bad/tree.md`](bad/tree.md) - `order/api/`, `order/web/`, 한 폴더에 섞인 서비스·엔티티·설정, `persistence/` 안의 객체 변환 `PaymentMapper`, `common/util/`.
- [`bad/PaymentCreationService.java`](bad/PaymentCreationService.java) - 결제 서비스가 주문의 `adapter/out/persistence` 엔티티와 리포지토리를 바로 쓴다.
- [`bad/PaymentJpaEntity.java`](bad/PaymentJpaEntity.java) - 다른 기능 엔티티에 `@ManyToOne` 을 걸고 `@Getter` 로 연다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `order/api/` 에 공개 값과 입구 | 공개 값은 `order/domain/`, 입구는 `order/application/port/in/` | 뭉뚱그린 폴더 이름(`api`, `common`, `util` 등)이 없다 |
| `OrderService` 안의 `@Scheduled`, `@EventListener` 메서드 | `adapter/in/schedule/UnpaidOrderCancelSchedule`, `adapter/in/event/ReceiptOnOrderPlacedListener` 가 `port/in` 을 부르고, 스케줄러 실행기 조립은 `config/OrderSchedulingConfig` | 입구가 인바운드 어댑터 역할 폴더에만 있고, 잡·스케줄러 조립은 `config` 에 있다 |
| `order/` 한 폴더에 `OrderService`, `OrderJpaEntity`, `OrderConfig` | `application/service`, `adapter/out/persistence`, `config` 로 나눔 | 한 폴더에 컨트롤러·설정·엔티티·서비스가 섞여 있지 않다 |
| 결제 서비스가 `OrderJpaRepository`, `OrderJpaEntity` import | `GetOrderSummaryUseCase` 와 `OrderSummary` 만 import | 다른 기능의 `adapter`, 영속 엔티티, `service` 구현을 import 하지 않는다 |
| `@ManyToOne OrderJpaEntity order` + `@Getter` | `long orderId` 키 칸 | 다른 기능 엔티티는 키 칸으로만 들고, 연관도 상대 표와 잇는 쿼리도 없다 |
| `persistence/PaymentMapper`(객체 변환 클래스) | `PaymentJpaEntity.of(payment)` | `persistence/` 의 매퍼가 SQL 매퍼뿐이고 객체 변환 Mapper 클래스가 없다 |
| `common/util/DateUtil` | 주인 기능으로 옮기고 진짜 공용만 `shared/` | 뭉뚱그린 폴더 이름이 없다 |

- 내 엔티티의 메서드가 주문 값으로 판단해야 하는 경우에도 연관을 걸지 않는다. 서비스가 주문 `port/in` 에서 그 값을 받아 메서드 인자로 넘긴다. 여러 결제와 주문을 짝 지어 판단하면 그 짝은 일급 컬렉션이 든다(ADR-002).
- 기존 저장소가 다른 배치(예: 성격별 최상위)를 골랐다면 판정 기준은 그 배치다. 이 트리는 새 저장소의 기본 배치다.

## 기계 검사

[`checks/AdrArchitectureTest.java`](../../checks/README.md) 의 `dependenciesPointInward` 가 세 층(`adapter`, `application`, `domain`) 사이의 역방향 의존을 막는다.
`bad/PaymentCreationService.java` 는 `payment.application` 이 `order.adapter` 를 부르므로 여기에 걸린다.
같은 층 안의 기능 사이 호출(예: `payment.adapter` → `order.adapter`), 폴더 이름, 한 폴더 한 성격, 연관의 getter 는 리뷰가 체크리스트로 본다.

## 공개 참고

- thombergs/buckpal [패키지](https://github.com/thombergs/buckpal/tree/master/src/main/java/io/reflectoring/buckpal) - `adapter/in`·`out`, `application/port/in`·`out` 이 기본 배치와 가깝다.
  - 우리 ADR 과 다른 점: `domain` 의 자리, `common/` 폴더, `AccountMapper`(객체 변환 Mapper), 2024 이후 갱신이 없다.
- spring-modulith [예제](https://github.com/spring-projects/spring-modulith/tree/main/spring-modulith-examples/spring-modulith-example-full/src/main/java/example) - 최상위를 기능으로 나눈다.
  - 우리 ADR 과 다른 점: 기능 안이 `internal/` 하나다(성격별 폴더가 없다).
