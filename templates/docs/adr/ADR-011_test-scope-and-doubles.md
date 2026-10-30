# ADR-011: 테스트는 범위를 나누고, 테스트 더블은 바깥 의존에만 쓴다

## Status

Accepted

## Date

2026-10-30

## Intent

테스트가 빠르고 결정적이게 하고, 목(mock)이 아니라 실제 동작을 검증하게 한다.

## Context

- 기존 상태: 모든 테스트가 애플리케이션 컨텍스트 전체를 띄운다. 도메인 객체까지 목으로 바꿔 "이 메서드가 불렸다" 만 단언한다. 비동기 결과를 `Thread.sleep` 으로 기다린다.
- 문제점: 테스트가 느려 자주 안 돌린다. 목만 검증해 구현을 바꾸면 테스트가 깨지고, 실제 버그는 못 잡는다. `sleep` 은 느리고 가끔 실패한다.
- 제약 조건: DB·메시지 브로커처럼 진짜와 대역의 동작이 다른 것은 통합 테스트에서 진짜(또는 컨테이너)로 확인해야 한다.

## Decision

**범위를 나눠 쓰고, 대역은 경계 밖에만 둔다.**

- 범위:

  | 범위 | 무엇 | 컨텍스트 |
  |---|---|---|
  | 단위 | 도메인 값과 판단, 서비스 흐름 | 띄우지 않는다. `new` 로 만든다(ADR-007) |
  | 통합 | 어댑터와 바깥(DB, HTTP, 파일), 설정 조립 | 필요한 만큼만 띄운다. 진짜 DB 나 컨테이너 |

- 테스트 더블은 바깥 의존(포트 뒤의 외부 API, 시계, 난수, 메시지 발행)에만 쓴다. 도메인 객체와 값은 진짜를 쓴다.
- 목이 불렸는지만 단언하지 않는다. 결과 값이나 남은 상태를 단언한다. 호출 검증은 그것 자체가 계약일 때(예: 외부로 한 번만 보낸다)만.
- 기대값은 리터럴로 쓴다. 대상 코드와 같은 계산으로 기대값을 만들지 않는다.
- 시각은 고정한 `Clock` 을 넣는다(ADR-002). 비동기는 `Thread.sleep` 대신 조건을 기다리는 도구(Awaitility 등)로.
- 테스트 하나는 한 가지 동작을 본다. 이름이 그 동작을 말한다.

### 체크리스트

- [ ] 도메인·서비스 단위 테스트가 애플리케이션 컨텍스트를 띄우지 않는다
- [ ] 도메인 객체·값을 목으로 바꾸지 않는다
- [ ] `verify` 만 있고 결과 단언이 없는 테스트가 없다
- [ ] 테스트에 `Thread.sleep` 이 없다
- [ ] 기대값을 대상 코드로 계산하지 않는다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| 서비스 하나 보려고 전체 컨텍스트 테스트 | `new OrderService(fakePort, fixedClock)` |
| `when(order.total()).thenReturn(...)`(값 객체 목) | 진짜 `Order` 를 만든다 |
| `verify(repository).save(any())` 만 | 저장된 값이나 돌려준 결과를 단언 |
| `assertEquals(price * qty, result)` | `assertEquals(Money.of(3000), result)` |
| `Thread.sleep(2000)` 뒤 확인 | `await().atMost(...).until(...)` |

## Consequences

### 긍정적
- 단위 테스트가 빨라 자주 돌린다.
- 구현을 바꿔도 동작이 같으면 테스트가 안 깨진다.
- 실패가 가끔 나는 테스트가 줄어든다.

### 부정적
- 통합 테스트를 따로 갖추는 비용(컨테이너 등)이 든다.
- 바깥 의존에 쓸 가짜 구현(fake)을 만들어 둬야 한다.

## References

- 출처: [affaan-m/everything-claude-code java-coding-standards](https://github.com/affaan-m/everything-claude-code/blob/main/skills/java-coding-standards/SKILL.md), [affaan-m/everything-claude-code java-reviewer.md](https://github.com/affaan-m/everything-claude-code/blob/main/agents/java-reviewer.md), [obra/superpowers writing-good-tests.md](https://github.com/obra/superpowers/blob/main/skills/test-driven-development/writing-good-tests.md), [github/awesome-copilot code-review-generic.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/code-review-generic.instructions.md)
- ADR-002(Clock), ADR-004(포트), ADR-007(생성자 주입)
