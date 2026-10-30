# ADR-014: 공유 상태를 두지 않고, 스레드는 정해진 풀에서, 중단을 삼키지 않는다

## Status

Accepted

## Date

2026-10-30

## Intent

여러 요청·작업이 동시에 돌아도 값이 섞이거나 스레드가 끝없이 늘지 않게 한다.

## Context

- 기존 상태: 싱글턴 빈에 요청마다 바뀌는 필드를 둔다. `@Async`, `CompletableFuture` 를 기본 실행기로 쓴다. `InterruptedException` 을 잡고 아무것도 안 한다.
- 문제점: 동시 요청에서 다른 요청의 값이 섞인다. 기본 실행기가 스레드를 제한 없이 만들거나 공용 풀을 막아 다른 작업까지 멈춘다. 중단 신호를 삼키면 종료가 안 된다.
- 제약 조건: 동시성 문제는 테스트에서 잘 재현되지 않아 규칙과 리뷰로 막아야 한다.

## Decision

**싱글턴은 무상태로, 실행기는 크기를 정해서, 중단은 전파한다.**

- 싱글턴 빈(서비스, 어댑터)에 요청·작업마다 바뀌는 가변 필드를 두지 않는다. 상태는 메서드 지역 변수나 인자로 넘긴다.
- 꼭 공유해야 하는 상태는 원자 타입·동시성 컬렉션·잠금으로 다루고, 그 이유를 주석으로 남긴다(ADR-009).
- `@Async`, `CompletableFuture.supplyAsync` 는 크기와 큐 상한이 정해진 실행기를 넘겨 쓴다. 기본 실행기·공용 풀에 맡기지 않는다.
- `InterruptedException` 을 잡으면 중단 표시를 되살리고(`Thread.currentThread().interrupt()`) 작업을 끝내거나 사유 예외로 바꿔 던진다. 삼키지 않는다(ADR-005).
- 바깥 호출에는 시간 제한을 둔다. 제한 없는 대기는 스레드를 묶는다.

### 체크리스트

- [ ] 싱글턴 빈에 요청마다 바뀌는 가변 필드가 없다
- [ ] `@Async`, `CompletableFuture` 가 정해진 실행기를 쓴다
- [ ] `InterruptedException` 을 잡고 아무것도 안 하는 `catch` 가 없다
- [ ] 공유 상태에 원자 타입·잠금과 이유 주석이 있다
- [ ] 바깥 호출에 시간 제한이 있다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| 서비스 빈에 `private Order current;` | 메서드 인자·지역 변수로 넘긴다 |
| `CompletableFuture.supplyAsync(task)`(공용 풀) | `supplyAsync(task, boundedExecutor)` |
| `@Async` 를 기본 실행기로 | 크기·큐 상한을 정한 실행기 빈을 지정 |
| `catch (InterruptedException e) {}` | `Thread.currentThread().interrupt();` 후 끝내거나 던진다 |
| 시간 제한 없는 HTTP 호출 | 연결·읽기 시간 제한 설정 |

## 다른 언어에서

- 원칙(공유 가변 상태 금지, 동시 실행 수 상한, 취소 신호 전파, 시간 제한)은 같다.
- Kotlin: 코루틴은 구조화된 동시성(정해진 스코프·디스패처), `CancellationException` 을 삼키지 않는다.
- TypeScript(Node): 단일 스레드라도 동시 요청 수에 상한(p-limit 등)을 두고, `AbortController` 로 취소·시간 제한을 넘긴다. 모듈 전역 가변 상태를 요청 사이에 공유하지 않는다.
- Python: asyncio 는 세마포어로 상한, `CancelledError` 를 삼키지 않는다. 스레드 풀은 `max_workers` 를 정한다.

## Consequences

### 긍정적
- 동시 요청에서 값이 섞이는 버그가 줄어든다.
- 스레드 수가 정해져 부하가 몰려도 앱이 버틴다.
- 종료 신호에 제때 멈춘다.

### 부정적
- 실행기 크기와 큐 상한을 정하고 운영에서 조정해야 한다.

## References

- 출처: [jabrena/plinth 125-java-concurrency](https://github.com/jabrena/plinth/blob/main/skills/125-java-concurrency/SKILL.md), [affaan-m/everything-claude-code java-reviewer.md](https://github.com/affaan-m/everything-claude-code/blob/main/agents/java-reviewer.md)
- ADR-005, ADR-008, ADR-009
