# ADR-001: 입력을 받은 자리부터 사람이 따라 읽을 수 있게 짠다

## Status

Accepted

## Date

2026-11-03

## Intent

입력(요청, 실행 명령, 메시지)을 받은 자리에서 읽기 시작한 사람이, 이름만 보고 각 단계가 무슨 일을 하는지 알며 끝까지 따라갈 수 있게 한다.

이 ADR 이 나머지의 전제다. 코드를 누가 짜든(사람이든 AI 든) 결국 사람이 읽고 확인한다. 따라 읽을 수 없으면 다른 ADR 을 지켰는지도 확인할 수 없다.

## Context

- 기존 상태: 기능을 붙일 때마다 클래스, 래퍼, 콜백이 늘고, 그 이름이 맡은 일 대신 구현 수단(잡아 둠, 감쌈, 흘려보냄)을 말한다.
- 문제점: 입구를 열면 `stream(runs.x(key), response)`, `claim()`, `holding()` 처럼 읽어도 뜻을 모르는 호출이 이어진다. 안을 다 열어야 흐름이 보이고, 실제 처리는 그 안쪽에 묻힌다.
- 제약 조건: 클래스 수, 층 수, 위임 자체는 문제가 아니다. 층 나눔(ADR-005)과 판단의 소유(ADR-002)는 그대로 지킨다.

## Decision

**입력을 받은 자리에서 이름만 읽어 내려가도 "이 흐름에서는 이 일을 하는구나" 를 알 수 있게 짠다.**

- **누가 판정하나.** 그 코드를 맡긴 사람(코드의 주인)이 읽히는지가 기준이다. 그 사람이 코드를 보고 업무를 이해해야 하기 때문이다. 주인이 따라가지 못한 자리는 고친다. 흐름 글을 적는 PR 에서는 그것을 판정 근거로 삼는다(적용 대상과 적는 법은 ADR-020).
- **이름은 호출식 전체로 본다.** 받는 객체, 메서드, 인자를 업무 용어와 함께 읽는다. `OrderResponse.of(order)`, `orders.findById(orderId)` 처럼 규칙 이름(`of`, `get`, `find`)이 들어간 호출식도 전체가 뜻을 말하면 괜찮다.
- **흐름의 단계는 맡은 일을 말한다.** 구현 수단만 말하는 이름(`run`, `handle`, `process`, `execute`, `claim`, `holding`, `stream`)으로 단계를 나타내지 않는다. 프레임워크 계약 메서드(`Runnable.run`, `Tasklet.execute`)는 예외이고, 그것을 부르는 우리 쪽 호출식이 맡은 일을 말하면 된다.
- **익숙한 구조를 드러낸다.** Controller → Service, Job → Step, Listener → Handler 처럼 프레임워크를 아는 사람이 기대하는 구조가 코드에서 그대로 보이게 둔다. 프레임워크가 주는 것을 같은 일의 자체 타입으로 다시 감싸지 않는다.
- **이벤트, 콜백, 넘기는 작업, 파이프라인도 쓴다.** 등록·발행하는 자리에서 무슨 일이 언제 도는지 보이면 된다.
  - 이벤트 타입은 일어난 사실을, 받는 쪽은 하는 일을 말한다(`OrderPlacedEvent` → `sendReceiptOnOrderPlaced`).
  - 언제 도는지는 호출식, 리스너 선언, 등록 설정에서 보이면 된다(`@TransactionalEventListener(phase = AFTER_COMMIT)`, `@Async("mailExecutor")`).
  - 파이프라인은 전체에 업무 목적 이름을 주고(`buildUnpaidReminderMails(orders)`), 뜻이 안 보이는 연산만 뽑아 이름을 붙인다.
- **금지하는 것은 하나다.** 이름으로 내용을 알 수 없는 실행 객체(람다를 든 홀더, "잡아 둔 실행")를 돌려줘, 호출한 쪽이 그 안을 뒤져야 무엇이 실행되는지 아는 구조.
- **ADR-016 과 나눔.** 낱말 하나가 뜻을 드러내는지는 ADR-016, 단계를 이어 읽을 때 흐름이 보이는지는 이 ADR 이 본다. 한 자리는 하나로만 지적한다.

### 체크리스트

- [ ] 코드의 주인이 코드 이름만으로(흐름 글을 적는 PR 이면 그 글과 함께) 흐름을 따라간다
- [ ] 흐름의 단계를 나타내는 호출식이 맡은 일을 말한다(프레임워크 계약 메서드는 예외)
- [ ] 이벤트, 콜백, 넘기는 작업은 등록·발행하는 자리에서 무슨 일이 언제 도는지 보인다
- [ ] 이름으로 내용을 알 수 없는 실행 객체를 돌려줘 안을 뒤지게 하는 구조가 없다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `stream(runs.report(key), response)`, `slot.claim()` | `reportJobService.startReportJobAndWriteProgress(targetKey, progress)`, `runningFlag.markStartedOrReject()` |
| 람다를 든 실행 객체를 돌려주고 위로 돌아가 다른 자리에서 실행 | 그 단계에서 이름 있는 메서드를 부른다. 미뤄야 하면 이름 있는 작업(`sendReceiptTask`)이나 이벤트로 |
| `DataChanged` 이벤트와 `onEvent` 리스너 | `OrderPlacedEvent` 와 `sendReceiptOnOrderPlaced` |
| 층마다 `process()`, `handle()`, `execute()` | 층마다 하는 일(`validateTarget`, `startReportJob`, `writeResultLine`) |

## 다른 언어에서

- 원칙은 같다. Kotlin 은 스코프 함수(`let`, `also`, `run`)를 겹쳐 흐름을 가리지 않고, TypeScript 는 미들웨어·데코레이터·이벤트 이름이, Python 은 데코레이터·콜백·`create_task` 에 넘기는 코루틴 이름이 맡은 일을 말하게 한다. 표준 라이브러리 함수 이름은 위 금지와 상관없다.

## Consequences

### 긍정적
- 코드의 주인이 입력부터 흐름을 따라가고, 그 흐름으로 업무를 이해한다.
- AI 가 짠 코드도 사람이 확인할 수 있고, 그래야 나머지 ADR 도 확인된다.

### 부정적
- 이름이 길어지고 짓는 데 시간이 든다. 좋은 이름이 안 나오면 구조가 이상하다는 신호다.
- 판정이 주인의 판단에 기댄다. 기계 검사는 안 된다.

## 다시 볼 때

처음 적용하는 프로젝트에서 주인이 따라가지 못한 자리를 모아, 이 ADR 의 기준과 예시를 고친다.

## References

- Robert C. Martin, *Clean Code* 2장 "의미 있는 이름", 3장 "함수"(내려가기 규칙)
- John Ousterhout, *A Philosophy of Software Design* 의 "얕은 모듈", "좋은 이름 고르기"
- ADR-002, ADR-005, ADR-014(커밋 뒤 리스너), ADR-015(실행기), ADR-016(이름), ADR-020(흐름 글), ADR-021(선언적 문법)
- 예시: [API 로 배치 잡을 실행하는 흐름](examples/readable-flow-batch.md) - 읽기 어려운 모양과 따라 읽히는 모양, 공개 참고 코드(Spring Batch 공식 샘플, spring-petclinic)
