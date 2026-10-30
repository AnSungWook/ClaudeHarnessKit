# ADR-010: 로그는 수준을 뜻대로 쓰고, 한 번만, 안전하게 남긴다

## Status

Accepted

## Date

2026-10-30

## Intent

운영에서 로그만 보고 무슨 일이 났는지, 무엇을 해야 하는지 알게 하고, 로그가 개인정보·비밀값을 흘리지 않게 한다.

## Context

- 기존 상태: 모든 것을 INFO 나 ERROR 로 찍는다. 문자열을 `+` 로 이어 붙인다. 예외를 잡을 때마다 로그를 찍고 다시 던져 같은 예외가 여러 번 찍힌다. 요청 본문을 통째로 남긴다.
- 문제점: 경보가 너무 많아 진짜 문제를 놓친다. 같은 실패가 여러 줄로 보여 원인 찾기가 어렵다. 로그 저장소에 개인정보·토큰이 쌓인다.
- 제약 조건: 운영 로그 문구는 모니터링·경보 규칙이 기대는 계약이 되기도 한다.

## Decision

**수준은 뜻대로, 예외는 한 번만, 민감한 값은 남기지 않는다.**

- 수준의 뜻:

  | 수준 | 언제 |
  |---|---|
  | ERROR | 사람이 조치해야 한다(작업 실패, 데이터 불일치) |
  | WARN | 스스로 복구했거나 곧 문제가 될 수 있다(재시도 성공, 한도 근접) |
  | INFO | 업무 사건(주문 생성, 배치 시작·끝과 건수) |
  | DEBUG | 진단용 상세. 운영 기본값에서는 꺼져 있다 |

- 메시지는 매개변수(`log.info("Order paid: orderId={}", id)`)로 쓴다. 문자열 연결로 만들지 않는다. 키=값 꼴로 써서 검색이 되게 한다.
- 예외는 처리하는 경계에서 한 번만 스택과 함께 남긴다(ADR-005). 중간에서 "찍고 다시 던지기" 를 하지 않는다.
- 남기지 않는 것: 비밀번호, 토큰, API 키, 주민·카드번호 같은 개인정보, 요청·응답 본문 통째로. 필요하면 가린 값이나 식별자만.
- `System.out`, `printStackTrace()` 를 쓰지 않는다.
- 운영이 기대는 로그 문구(경보 규칙이 거는 문구)를 바꿀 때는 그 규칙을 함께 고친다.

### 체크리스트

- [ ] `System.out`, `printStackTrace` 가 없다
- [ ] 로그 메시지에 문자열 `+` 연결이 없다
- [ ] 같은 예외를 잡아 로그를 찍고 다시 던지는 자리가 없다
- [ ] 토큰·비밀번호·개인정보·본문 전체를 로그에 넣지 않는다
- [ ] ERROR 는 조치가 필요한 경우에만 쓴다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `log.info("주문 " + id + " 결제")` | `log.info("Order paid: orderId={}", id)` |
| `catch (e) { log.error("실패", e); throw e; }` 를 층마다 | 경계에서 한 번만 남긴다 |
| 재시도해 성공한 건을 ERROR 로 | WARN(복구됨) 또는 INFO |
| `log.debug("request={}", requestBody)` 에 토큰 포함 | 식별자만, 민감 칸은 가린다 |
| `e.printStackTrace()` | `log.error("...: key={}", key, e)` 를 경계에서 |

## Consequences

### 긍정적
- 경보가 줄고 남은 경보는 조치가 필요하다.
- 한 실패가 한 줄로 보여 원인을 빨리 찾는다.
- 로그 저장소가 민감 정보 유출 경로가 되지 않는다.

### 부정적
- 수준 판단이 리뷰에 기댄다.
- 문구를 계약으로 다루면 바꾸는 비용이 생긴다.

## References

- 출처: [jabrena/plinth 181-java-observability-logging](https://github.com/jabrena/plinth/blob/main/skills/181-java-observability-logging/references/181-java-observability-logging.md), [github/awesome-copilot springboot.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/springboot.instructions.md), [affaan-m/everything-claude-code java-reviewer.md](https://github.com/affaan-m/everything-claude-code/blob/main/agents/java-reviewer.md), [github/awesome-copilot code-review-generic.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/code-review-generic.instructions.md)
- ADR-005 도메인 사유 예외(경계에서만 잡는다)
