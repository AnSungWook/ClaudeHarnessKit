# ADR-020: 커밋 제목은 `type(scope): subject` 이고, 본문과 PR 은 왜와 돌린 검증을 든다

## Status

Accepted

## Date

2026-11-02

## Intent

`git log` 한 줄로 무엇이 어느 모듈에서 왜 바뀌었는지 읽히게 하고, 리뷰하는 사람이 무엇을 확인했고 무엇을 안 했는지 PR 에서 바로 알게 한다.

## Context

- 기존 상태: 커밋 제목이 사람마다 평서문, 영어, 이슈 번호만, `[AI]` 같은 접두로 섞인다. 본문이 비어 있다. PR 설명이 "기능 추가" 한 줄이다.
- 문제점: 같은 종류의 변경이 다른 모양으로 남아 로그로 가를 수 없다. 왜 바꿨는지가 사라진다. PR 에서 테스트를 돌렸는지, 정적 분석이 어땠는지 알 수 없어 리뷰가 그것부터 묻는다.
- 제약 조건: 브랜치 모델과 머지 방식(머지 커밋, squash, rebase)은 프로젝트마다 다르다. 이 ADR 은 커밋과 PR 의 글만 정한다.

## Decision

**커밋 제목은 정해진 모양 한 줄, 본문과 PR 은 왜와 검증을 든다.**

- 커밋 제목은 `type(scope): subject` 다.
  - type 은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build` 가운데 하나다.
  - scope 는 모듈이나 기능 이름(영문 소문자와 하이픈)이고 저장소 전체면 뺀다.
  - subject 는 팀의 언어로 무엇을 했는지 쓰고 마침표를 찍지 않는다.
  - 접두(`[AI Generated]`, `[WIP]`)는 붙이지 않는다. 누가 썼는지는 작성자와 `Co-Authored-By` 가 든다.
- 커밋 본문은 왜 바꿨는지와 돌린 검증, 그 결과를 적는다. 에이전트가 만든 커밋은 본문 끝에 `Co-Authored-By:` 로 도구를 적는다.
- PR 본문은 이것을 든다.
  - 변경 요약(무엇을, 왜). 여러 개면 표나 불릿으로.
  - 돌린 검증 명령과 결과. 정적 분석은 심각한 것(blocker, critical, major) 건수부터.
  - 돌리지 않은 검증은 "안 함" 과 이유.
  - 남긴 것(후속 일).
  - 입구(컨트롤러, 잡, 리스너, 메시지 수신)를 새로 만들거나 그 아래 업무 단계의 순서·구성을 바꿨으면 **흐름 글**(ADR-001). 이름만 바꾼 PR, 단계 안쪽만 고친 PR 은 적지 않는다.
    - 입구에서 업무 결과(저장, 응답, 발행)까지 업무 단계의 호출식을 한 줄에 하나씩, 안쪽 단계는 들여쓴다.
    - 분기는 조건을, 비동기 후속 처리(이벤트, 넘기는 작업)는 언제 도는지와 받는 쪽 단계까지 적는다.
    - 접근자, 단순 변환, 라이브러리 내부 호출은 적지 않는다.

    ```text
    POST /orders → orderController.placeOrder(request)
      orderService.placeOrder(command)
        stock.reserve(items)            재고 부족이면 OutOfStockException(409)
        orders.save(order)
        events.publishEvent(OrderPlacedEvent.of(order))
    커밋 뒤 → receiptListener.sendReceiptOnOrderPlaced(event)   mailExecutor 에서
    ```
- 머지 커밋 제목은 도구가 만든 그대로 둔다.
- 지난 커밋은 고치지 않는다. 이 ADR 을 들인 뒤 커밋부터 맞춘다.

### 체크리스트

- [ ] 커밋 제목이 `type(scope): subject` 모양이고 type 이 목록 안이다
- [ ] 제목에 접두가 없고 마침표로 끝나지 않는다
- [ ] 본문에 왜와 돌린 검증이 있다
- [ ] PR 본문에 변경 요약, 검증 결과, 안 한 검증, 남긴 것이 있다
- [ ] 입구를 만들거나 업무 단계를 바꾼 PR 에 흐름 글이 있고, 코드의 호출식 이름과 맞는다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `수정`, `fix bug`, `WIP` | `fix(order): 취소한 주문이 정산에 다시 잡히던 것을 막는다` |
| `[AI Generated] feat: ...` | `feat(order): ...` + 본문 끝 `Co-Authored-By:` |
| 본문 없음 | 왜 바꿨는지 한두 줄과 `./gradlew test` 결과 |
| PR 설명 "기능 추가" | 변경 표, 돌린 검증과 결과, 안 한 검증, 남긴 것 |
| 정적 분석 "통과" 한 단어 | `blocker 0, critical 0, major 2(고침)` 처럼 건수부터 |
| 흐름을 바꾼 PR 에 "배치 실행 구조 개선" 한 줄 | 입구부터 업무 결과까지 호출식 흐름 글 |

## 다른 언어에서

- 언어와 무관하다. Conventional Commits 와 같은 모양이라 commitlint 같은 도구로 기계 검사를 걸 수 있다.

## Consequences

### 긍정적
- 로그만으로 모듈별, 종류별 변경을 가를 수 있다(배포 노트, 반입분 가르기).
- 리뷰가 "테스트 돌렸어요?" 를 묻지 않는다.

### 부정적
- 커밋과 PR 을 쓰는 데 시간이 조금 더 든다.

## References

- [Conventional Commits](https://www.conventionalcommits.org/)
- ADR-007(바꾼 줄만 고친다 - 모양만 바꾼 커밋을 기능 커밋과 섞지 않는다)
- ADR-001(흐름 글은 사람이 따라 읽을 수 있는지 판정하는 근거)
