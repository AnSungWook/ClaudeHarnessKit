# ADR-015: 공유 상태를 두지 않고, 스레드는 정해진 풀에서, 중단을 삼키지 않는다

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
- 꼭 공유해야 하는 상태는 원자 타입·동시성 컬렉션·잠금으로 다루고, 그 이유를 주석으로 남긴다(ADR-010).
- `@Async`, `CompletableFuture.supplyAsync` 는 크기와 큐 상한이 정해진 실행기를 넘겨 쓴다. 기본 실행기·공용 풀에 맡기지 않는다.
  - 비동기 이벤트 리스너도 같다. 리스너마다 `@Async("<실행기 이름>")` 로 정해진 실행기를 쓰고, 전역 이벤트 멀티캐스터에 실행기를 걸어 모든 리스너를 한꺼번에 비동기로 바꾸지 않는다 - 어느 리스너가 비동기인지 선언에서 안 보이게 된다(ADR-001). 큐가 차서 거부될 때와 비동기 예외를 어떻게 다루는지도 정한다.
- `InterruptedException` 을 잡으면 중단 표시를 되살리고(`Thread.currentThread().interrupt()`) 작업을 끝내거나 사유 예외로 바꿔 던진다. 삼키지 않는다(ADR-006).
- 바깥 호출에는 시간 제한을 둔다. 제한 없는 대기는 스레드를 묶는다(HTTP 는 ADR-018).
- 잠금(`synchronized`, `ReentrantLock`)은 JVM 하나 안의 상태에만 쓴다(메모리 캐시, 집계, 서버마다 따로 쓰는 파일). 여러 서버가 같이 쓰는 데이터(DB 행, 공유 저장소 파일, 채번)는 DB 잠금(행 잠금, `SKIP LOCKED`)이나 저장소의 원자적 생성으로 다룬다. 공유 데이터에 JVM 잠금은 서버가 한 대(이중화해도 Active-Standby 라 한 대만 처리)임이 보장될 때만 쓰고, 그 이유를 주석에 남긴다. 보장 없이 지금 한 대라는 이유로 쓰면 서버를 늘리는 날 조용히 깨진다.
- 런타임이 Java 21~23 이면 `synchronized` 안에서 파일, DB, 네트워크 호출과 `Thread.sleep`, `Object.wait` 를 하지 않는다. 가상 스레드가 캐리어 스레드를 붙잡는다(pinning). 그런 일을 잠가야 하면 `ReentrantLock` 이다. Java 24 이상은 `synchronized` 가 캐리어를 붙잡지 않으니(JEP 491) 이 이유로 바꾸지 않는다.

### 체크리스트

- [ ] 싱글턴 빈에 요청마다 바뀌는 가변 필드가 없다
- [ ] `@Async`, `CompletableFuture` 가 정해진 실행기를 쓴다
- [ ] `InterruptedException` 을 잡고 아무것도 안 하는 `catch` 가 없다
- [ ] 공유 상태에 원자 타입·동시성 컬렉션·잠금과 이유 주석이 있다
- [ ] 바깥 호출에 시간 제한이 있다
- [ ] 여러 서버가 같이 쓰는 데이터를 JVM 잠금으로 지키지 않는다(DB 잠금이나 원자적 생성). 서버 한 대(Active-Standby 포함)가 보장돼 JVM 잠금을 쓴 자리는 이유 주석이 있다
- [ ] (Java 21~23) `synchronized` 블록 안에 파일, DB, 네트워크 호출과 `sleep`, `wait` 가 없다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| 서비스 빈에 `private Order current;` | 메서드 인자·지역 변수로 넘긴다 |
| `CompletableFuture.supplyAsync(task)`(공용 풀) | `supplyAsync(task, boundedExecutor)` |
| `@Async` 를 기본 실행기로 | 크기·큐 상한을 정한 실행기 빈을 지정 |
| `catch (InterruptedException e) {}` | `Thread.currentThread().interrupt();` 후 끝내거나 던진다 |
| 시간 제한 없는 HTTP 호출 | 연결·읽기 시간 제한 설정 |
| (Java 21~23) `synchronized (this) { Files.write(...); }` | `lock.lock(); try { Files.write(...); } finally { lock.unlock(); }` |
| 서버 한 대 보장 없이 `synchronized` 로 DB 채번이나 "한 건만 처리" 를 지키기 | 행 잠금(`SELECT ... FOR UPDATE SKIP LOCKED`), 유일 제약, 원자적 생성 |

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
- ADR-006, ADR-009, ADR-010
- 예시: [examples/015-concurrency-basics](examples/015-concurrency-basics/README.md) - 바른 모양(good)과 어긋난 모양(bad)
