# ADR-020 예시: 커밋 제목은 `type(scope): subject`, 본문과 PR 은 왜와 돌린 검증

[ADR-020](../../ADR-020_commit-and-pr-conventions.md) 을 "회원이 결제 전 주문을 취소하는 창구" 한 건의 커밋과 PR 에 적용한 예다.

## 무엇을 보여 주나

같은 변경을 두 모양으로 남겼다. 바른 모양은 제목 한 줄로 종류와 모듈과 할 일이 읽히고, 본문과 PR 에서 왜 바꿨는지, 무엇을 돌렸고 무엇을 안 돌렸는지 알 수 있다.
입구(컨트롤러)를 새로 만든 PR 이라 흐름 글도 든다.

## 바른 모양

- [`good/commit-message.txt`](good/commit-message.txt) - `feat(order): ...` 제목, 왜 두 줄, 검증 결과(안 한 것 포함), 끝에 `Co-Authored-By:`.
- [`good/pull-request.md`](good/pull-request.md) - 변경 표, 흐름 글, 돌린 검증(정적 분석은 심각한 것 건수부터), 안 한 검증과 이유, 남긴 것.

## 어긋난 모양

- [`bad/commit-message.txt`](bad/commit-message.txt) - `[AI Generated]` 접두, type 과 scope 가 없음, 마침표로 끝남, 본문 없음.
- [`bad/pull-request.md`](bad/pull-request.md) - "기능 추가" 한 줄, 무슨 테스트를 돌렸는지 모름, 정적 분석 "통과" 한 단어, 안 한 검증과 흐름 글 없음.

이 밖에 흔한 어긋난 제목: `수정`, `fix bug`, `WIP`, `Feat(Order): ...`(type 대문자), `feat(Order-API): ...`(scope 대문자), `perf(order): ...`(목록 밖 type).

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `[AI Generated] 주문 취소 기능 추가.` | `feat(order): 결제 전 주문을 회원이 취소하는 창구를 더한다` | 커밋 제목이 `type(scope): subject` 이고 type 이 목록 안이다 / 접두가 없고 마침표로 끝나지 않는다 |
| 본문 없음 | 왜 두 줄과 `./gradlew :order:test` 결과, 안 한 통합 테스트 | 본문에 왜와 돌린 검증이 있다 |
| 접두로 도구 표시 | 본문 끝 `Co-Authored-By:` | 제목에 접두가 없다 |
| PR "기능 추가했습니다" | 변경 표, 검증 결과, 안 한 검증, 남긴 것 | PR 본문에 변경 요약, 검증 결과, 안 한 검증, 남긴 것이 있다 |
| "소나 통과" | `blocker 0, critical 0, major 0, minor 1(고침)` | (Decision: 정적 분석은 심각한 것 건수부터) |
| 새 창구인데 흐름 설명 없음 | 입구부터 저장, 발행, 커밋 뒤 리스너까지 호출식 흐름 글 | 입구를 만든 PR 에 흐름 글이 있고, 코드의 호출식 이름과 맞는다 |

- 흐름 글의 호출식(`orderCancellationService.cancelOrder(command)`, `order.cancel()`, `orders.save(...)`)은 코드의 이름과 같아야 한다. 이름만 바꾼 PR, 단계 안쪽만 고친 PR 은 흐름 글을 적지 않는다.
- `Co-Authored-By:` 의 도구 이름과 주소는 자리표시다. 실제로 쓴 도구를 적는다.

## 기계 검사

[`checks/commitlint.config.js`](../../checks/README.md) 가 커밋 제목을 막는다. `bad/commit-message.txt` 는 `type(scope): ` 모양이 아니라 해석이 안 돼 `type-empty`, `subject-empty` 에 걸린다(접두를 떼고 `feat(order): ... .` 로 써도 끝 마침표가 `subject-full-stop` 에 걸린다). 머지 커밋 말고는 무시하지 않는다.
본문의 왜와 검증, PR 본문은 리뷰가 체크리스트로 본다.

## 공개 참고

- angular [commit-message-guidelines](https://github.com/angular/angular/blob/main/contributing-docs/commit-message-guidelines.md) - `type(scope): summary` 모양.
  - 우리 ADR 과 다른 점: type 목록이 다르다(`perf`, `ci` 가 있고 `chore` 가 없다). 본문에 검증 결과를 요구하지 않는다.
