# ADR-013: 트랜잭션은 응용 서비스에 두고, 그 안에서 바깥을 부르지 않는다

## Status

Accepted

## Date

2026-10-30

## Intent

한 업무 단위가 함께 저장되거나 함께 취소되게 하고, 느린 바깥 호출이 DB 연결과 잠금을 붙잡지 않게 한다.

## Context

- 기존 상태: `@Transactional` 이 컨트롤러·리포지토리·서비스에 제각각 붙어 있다. 트랜잭션 안에서 외부 API·LLM·파일 업로드를 부른다.
- 문제점: 어디서 커밋되는지 몰라 일부만 저장되는 일이 생긴다. 바깥 호출이 느리면 그동안 DB 연결과 잠금이 묶여 연결 풀이 바닥난다. 바깥 호출은 롤백되지 않아 DB 와 어긋난다.
- 제약 조건: 바깥 호출 결과를 저장해야 하는 흐름은 "호출 → 짧은 트랜잭션으로 저장" 으로 나눠야 한다.

## Decision

**트랜잭션 경계는 응용 서비스의 업무 단위 하나다.**

- `@Transactional` 은 응용 서비스(ADR-004 의 서비스 층) 메서드에 둔다. 컨트롤러와 어댑터(리포지토리 포함)에는 두지 않는다.
- 읽기만 하는 메서드는 읽기 전용 트랜잭션(`readOnly = true`)으로 둔다.
- 트랜잭션 안에서 바깥(HTTP API, LLM, 메시지 발행, 파일·오브젝트 스토리지)을 부르지 않는다. 바깥 호출은 트랜잭션 밖에서 하고, 결과 저장만 짧은 트랜잭션으로 한다.
- 커밋 뒤에 해야 하는 일(이벤트 발행, 알림)은 커밋 뒤 실행(트랜잭션 이벤트 리스너, 아웃박스)으로 한다.
- 같은 클래스 안 메서드 호출은 프록시를 안 거쳐 트랜잭션이 안 걸린다는 점을 알고 경계를 정한다.

### 체크리스트

- [ ] 컨트롤러·리포지토리·어댑터에 `@Transactional` 이 없다
- [ ] 읽기 전용 서비스 메서드가 `readOnly` 다
- [ ] 트랜잭션 메서드 안에서 HTTP·LLM·메시지·스토리지를 부르지 않는다
- [ ] 커밋 뒤 해야 할 일이 커밋 전에 실행되지 않는다
- [ ] 같은 클래스 내부 호출로 트랜잭션을 기대하지 않는다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `@Transactional` 메서드 안에서 결제 API 호출 | 호출은 밖에서, 결과 저장만 트랜잭션으로 |
| 컨트롤러에 `@Transactional` | 서비스 메서드에 |
| 조회 메서드에 쓰기 트랜잭션 | `@Transactional(readOnly = true)` |
| 트랜잭션 안에서 메시지를 바로 발행 | 커밋 뒤 이벤트 리스너나 아웃박스 |
| `this.saveInternal()` 에 `@Transactional` 을 기대 | 경계를 바깥 메서드에 두거나 빈을 나눈다 |

## 다른 언어에서

- 원칙(업무 단위 하나가 트랜잭션, 그 안에서 바깥 호출 금지)은 같다. `@Transactional` 은 Spring 의 예다.
- TypeScript: ORM 의 트랜잭션 블록(`prisma.$transaction`, TypeORM `transaction`)을 응용 서비스에서만 연다.
- Python: `with session.begin():` 같은 트랜잭션 블록을 응용 서비스에서만 연다.

## Consequences

### 긍정적
- 업무 단위가 통째로 저장·취소된다.
- 느린 바깥 호출이 연결 풀과 잠금을 묶지 않는다.

### 부정적
- 바깥 호출 결과를 저장하는 흐름이 두 단계로 나뉘어, 중간 실패 처리(재시도, 보상)를 따로 설계해야 한다.

## References

- 출처: [affaan-m/everything-claude-code java-reviewer.md](https://github.com/affaan-m/everything-claude-code/blob/main/agents/java-reviewer.md), [jabrena/plinth 312-frameworks-spring-data-jdbc](https://github.com/jabrena/plinth/blob/main/skills/312-frameworks-spring-data-jdbc/references/312-frameworks-spring-data-jdbc.md)
- ADR-004 헥사고날 층
