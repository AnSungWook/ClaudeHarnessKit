# ADR-008: 불변을 먼저 쓴다

## Status

Accepted

## Date

2026-10-30

## Intent

값이 언제 누구에 의해 바뀌었는지 추적할 일을 없애고, 여러 스레드가 같은 값을 써도 안전하게 한다.

## Context

- 기존 상태: getter/setter 가 있는 가변 객체를 여기저기서 고치고, 받은 컬렉션을 그대로 수정한다. `Optional` 을 필드와 매개변수에 쓴다.
- 문제점: 값이 바뀐 자리를 찾으려면 호출 경로 전체를 봐야 한다. 공유된 가변 컬렉션은 동시성 버그를 낸다. 필드의 `Optional` 은 `null` 과 빈 값 두 가지 "없음" 을 만든다.
- 제약 조건: JPA 엔티티처럼 프레임워크가 가변을 요구하는 타입은 불변으로 둘 수 없다.

## Decision

**값은 불변으로 만들고, 바뀌는 상태는 꼭 필요한 곳에만 둔다.**

- 값 객체와 전달 객체는 record(또는 모든 필드 `final`)로. setter 를 두지 않는다. 바꾼 값이 필요하면 새 객체를 만든다(ADR-001 의 `withXxx`).
- 컬렉션은 불변으로 만들고 돌려준다(`List.of`, `List.copyOf`, `stream().toList()`). 받은 컬렉션을 고치지 않는다 - 필요하면 복사해서 고친다.
- record 가 가변 컬렉션을 받으면 생성자에서 `List.copyOf` 로 방어 복사한다.
- `Optional` 은 메서드 반환값에만 쓴다. 필드·매개변수·컬렉션 원소로 쓰지 않는다.
- 없음을 `null` 로 돌려주지 않는다. 컬렉션은 빈 컬렉션을, 하나짜리는 `Optional` 을 돌려준다. `Optional.get()` 대신 `orElseThrow(사유 예외)` 나 `orElse` 를 쓴다.
- 예외: JPA 엔티티 같은 프레임워크 가변 타입. 상태 변경은 이름 있는 메서드(`order.cancel()`)로만 하고 public setter 는 두지 않는다.

### 체크리스트

- [ ] 새 값·전달 타입이 record 이거나 모든 필드가 `final` 이다
- [ ] public setter 가 없다(엔티티 포함, 상태 변경은 이름 있는 메서드)
- [ ] 돌려주는 컬렉션이 불변이다
- [ ] 매개변수로 받은 컬렉션을 직접 고치지 않는다
- [ ] `Optional` 이 필드·매개변수에 없다
- [ ] 컬렉션을 돌려주는 메서드가 `null` 을 돌려주지 않는다
- [ ] `Optional.get()` 이 없다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `dto.setStatus(PAID)` 를 여러 곳에서 | `result.withStatus(PAID)` 로 새 값 |
| `return this.items;`(내부 가변 리스트 노출) | `return List.copyOf(items);` 또는 처음부터 불변 |
| `void process(List<Item> items) { items.removeIf(...); }` | 걸러낸 새 리스트를 돌려준다 |
| `private Optional<String> memo;` | `@Nullable String memo` 또는 별도 타입, 반환 때만 `Optional` |
| 엔티티에 `setStatus` public setter | `order.markPaid(paidAt)` 처럼 뜻 있는 메서드 |

## Consequences

### 긍정적
- 값이 바뀐 자리를 찾을 일이 줄어든다.
- 공유 상태로 인한 동시성 버그가 줄어든다.

### 부정적
- 바꿀 때마다 새 객체가 생긴다(대부분 무시할 수준).
- 엔티티는 예외로 남아 규칙이 둘이 된다.

## References

- 출처: [giuseppe-trisciuoglio/developer-kit](https://github.com/giuseppe-trisciuoglio/developer-kit/tree/main/plugins/developer-kit-java), [piomin/claude-ai-spring-boot](https://github.com/piomin/claude-ai-spring-boot)
- ADR-001 정적 팩토리와 책임(`withXxx`)
