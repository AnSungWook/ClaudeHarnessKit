# ADR-006: 코드 모양은 저장소에 커밋한 스타일을 따른다 - 와일드카드 import 없이, Reformat 은 바꾼 줄에만

## Status

Accepted

## Date

2026-10-30

## Intent

사람과 AI 가 같은 모양으로 코드를 쓰게 하고, 기능 변경 diff 에 모양만 바뀐 줄이 섞이지 않게 한다.

## Context

- 기존 상태: 개발자마다 IDE 기본 스타일이 달라 import 정리나 Reformat 을 돌릴 때마다 모양이 바뀐다. 기본 설정에서는 Optimize Imports 가 일정 개수 이상이면 `import x.*;` 로 합친다.
- 문제점: 파일 전체 Reformat 이 기능 변경과 섞이면 리뷰할 수 없는 큰 diff 가 생기고, `blame` 과 진행 중인 브랜치가 흔들린다. 와일드카드 import 는 이름 충돌과 의존 파악을 어렵게 한다(SonarQube S2208).
- 제약 조건: 포매터 플러그인을 빌드에 강제하면(Spotless 등) 처음에 저장소 전체 diff 를 한 번 내야 하고, 폐쇄망이면 반입할 의존이 는다.

## Decision

**저장소마다 IDE 프로젝트 코드 스타일(IntelliJ 는 `.idea/codeStyles/Project.xml`, `codeStyleConfig.xml`, 또는 `.editorconfig`)을 커밋하고 그것을 따른다.**

- 와일드카드 import 를 쓰지 않는다. 스타일 파일에서 와일드카드로 합치는 기준(`CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND`, `NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND`)을 999 로 둔다.
- Reformat 은 바꾼 줄에만(IntelliJ "Reformat → VCS changed text"). 파일 전체 Reformat 이나 모양만 바꾸는 diff 를 기능 변경과 섞지 않는다.
- 예외: 패키지 루트 변경처럼 어차피 모든 파일이 바뀌는 작업에서는 전체 Reformat 을 한 번 한다. 이때 이름 변경 커밋과 Reformat 커밋을 나눠, 리뷰가 둘을 따로 보게 한다.
- AI 에이전트에게도 같은 규칙을 준다 - 편집한 줄 밖의 모양을 바꾸지 않는다.

### 체크리스트

- [ ] `import .*\.\*;` 가 없다
- [ ] diff 에 기능과 무관한 모양만 바뀐 줄이 없다
- [ ] 저장소에 코드 스타일 파일이 커밋되어 있다
- [ ] 전체 Reformat 이 필요하면 별도 커밋으로 나뉘어 있다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `import java.util.*;` | 쓰는 타입마다 한 줄씩 |
| 기능 수정 PR 에서 파일 전체 Reformat | 바꾼 줄만 Reformat, 전체 정리는 별도 PR |
| IDE 기본 스타일에 맡김 | 저장소에 커밋한 스타일 파일을 따른다 |
| 이름 변경과 Reformat 을 한 커밋에 | 이름 변경 커밋, Reformat 커밋을 나눈다 |
| AI 가 파일을 다시 쓰며 들여쓰기·줄바꿈을 바꿈 | 편집한 줄만 바꾸도록 지시 |

## 다른 언어에서

- 원칙(저장소에 커밋한 스타일을 따르고, 바꾼 줄만 정리, 와일드카드 import 금지)은 같다. 도구만 다르다.
- TypeScript: `.editorconfig` + Prettier·ESLint 설정을 커밋. `import * as x` 를 남발하지 않는다.
- Python: `pyproject.toml` 의 ruff/black 설정을 커밋. `from x import *` 금지.
- 저장소 전체 포맷은 따로 한 번, 별도 커밋으로.

## Consequences

### 긍정적
- diff 가 작고 리뷰가 기능에 집중된다.
- 사람과 AI 가 같은 모양으로 쓴다.

### 부정적
- 지금 코드 모양과 스타일이 다른 곳은 한동안 섞여 있다(바꾼 줄부터 맞춰 간다).
- 빌드 강제가 없으므로 리뷰·정적 분석으로 지켜야 한다.

## References

- SonarQube 규칙 S2208(와일드카드 import)
