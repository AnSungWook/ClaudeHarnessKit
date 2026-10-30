# ADR-007: 의존은 생성자로 받고 final 로 둔다

## Status

Accepted

## Date

2026-10-30

## Intent

빈이 만들어지는 순간 필요한 의존이 다 갖춰져 있게 하고, 테스트에서 프레임워크 없이 객체를 만들 수 있게 한다.

## Context

- 기존 상태: `@Autowired` 필드 주입, setter 주입이 섞여 있다.
- 문제점: 필드 주입은 의존이 비어 있는 객체가 만들어질 수 있고, 테스트에서 리플렉션이나 컨테이너 없이는 넣을 수 없다. 의존이 몇 개인지 생성자에 안 드러나 클래스가 비대해져도 눈에 안 띈다.
- 제약 조건: 순환 의존을 setter 주입으로 풀던 코드가 있으면, 생성자 주입으로 바꿀 때 구조를 고쳐야 한다.

## Decision

**의존은 생성자로 받고 `final` 필드로 둔다.**

- `@Autowired` 필드 주입과 setter 주입을 쓰지 않는다. 생성자가 하나면 `@Autowired` 도 붙이지 않는다.
- 선택적 의존이 필요하면 `ObjectProvider` 나 `Optional` 생성자 인자로 받는다(필드를 `null` 로 두지 않는다).
- 생성자 인자가 많아지면(예: 6개 이상) 주입 방식이 아니라 클래스가 너무 많은 일을 하는 신호로 보고 나눈다.
- 순환 의존은 주입 방식으로 풀지 않고 구조(포트 분리, 이벤트)로 끊는다.

### 체크리스트

- [ ] 필드에 `@Autowired`·`@Inject` 가 없다
- [ ] 주입받는 필드가 모두 `final` 이다
- [ ] setter 주입이 없다
- [ ] 생성자 인자가 많은 클래스는 나눌 근거를 검토했다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `@Autowired private OrderRepository repository;` | `private final OrderRepository repository;` + 생성자 |
| `@Autowired public void setClient(...)` | 생성자 인자 |
| 순환 의존을 `@Lazy` 나 setter 로 우회 | 포트를 나누거나 흐름을 바꿔 순환을 없앤다 |
| 생성자 인자 10개를 그대로 둠 | 책임을 나눠 클래스를 쪼갠다 |

## 다른 언어에서

- Kotlin: 주 생성자의 `private val` 로 받는다.
- TypeScript(NestJS, Angular): 생성자 주입 + `private readonly`. 프레임워크 없는 코드는 생성자·함수 인자로 받는다.
- Python: `__init__` 인자로 받는다(FastAPI 의 `Depends` 도 같은 원칙). 모듈 전역 싱글턴을 직접 import 해 쓰지 않는다.

## Consequences

### 긍정적
- 의존이 빠진 객체가 만들어지지 않는다.
- 단위 테스트에서 `new` 로 바로 만든다.
- 의존 수가 생성자에 드러나 비대한 클래스를 일찍 알아챈다.

### 부정적
- 순환 의존을 쓰던 코드는 구조를 고쳐야 한다.

## References

- 출처: [giuseppe-trisciuoglio/developer-kit](https://github.com/giuseppe-trisciuoglio/developer-kit/tree/main/plugins/developer-kit-java), [piomin/claude-ai-spring-boot](https://github.com/piomin/claude-ai-spring-boot), [PatrickJS/awesome-cursorrules](https://github.com/PatrickJS/awesome-cursorrules/tree/main/rules)(kotlin-springboot)
- ADR-004 헥사고날 층과 의존 방향
