# ADR-007: 코드 모양은 저장소에 커밋한 스타일을 따른다 - 와일드카드 import 없이, Reformat 은 바꾼 줄에만

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
- 빌드 파일의 의존 목록은 자리, 종류, 알파벳 순으로 적는다(Gradle `dependencies` 예).
  - 자리는 위에서 아래로 `implementation` 라이브러리, `implementation project(...)`, `testImplementation` 라이브러리, `testImplementation testFixtures(project(...))`, `testImplementation project(...)`.
  - 그 밖의 설정은 Spring Initializr 가 만드는 순서로 그 사이에 둔다: `compileOnly`, `developmentOnly`, `runtimeOnly`, `annotationProcessor` 는 `implementation` 묶음 아래, `testCompileOnly`, `testRuntimeOnly`, `testAnnotationProcessor` 는 `testImplementation` 묶음 아래. `api` 는 `implementation` 바로 위.
  - 같은 자리 안은 알파벳 오름차순이다. 라이브러리는 `group:artifact` 좌표로, 모듈은 경로 문자열로 견준다.
  - 운영 묶음과 테스트 묶음 사이는 빈 줄 하나, 묶음 안에는 빈 줄을 두지 않는다. 줄 위 주석은 그 줄과 같이 움직인다.
  - 순서를 맞추는 커밋은 줄 순서만 바꾸고 의존을 더하거나 빼지 않는다. 처음부터 정렬 플러그인을 걸어 모든 파일을 흔드는 diff 를 내지 않는다.

  ```groovy
  dependencies {
      implementation 'org.jsoup:jsoup:1.22.2'
      implementation project(':common:core')
      implementation project(':order:domain')
      compileOnly 'org.postgresql:postgresql'

      testImplementation 'org.testcontainers:testcontainers-postgresql'
      testImplementation testFixtures(project(':common:testing'))
      testImplementation project(':payment:domain')
  }
  ```

### 체크리스트

- [ ] `import .*\.\*;` 가 없다
- [ ] diff 에 기능과 무관한 모양만 바뀐 줄이 없다
- [ ] 저장소에 코드 스타일 파일이 커밋되어 있다
- [ ] 전체 Reformat 이 필요하면 별도 커밋으로 나뉘어 있다
- [ ] 빌드 파일 의존 목록이 자리(라이브러리 → 모듈, 운영 → 테스트)와 알파벳 순이고, 운영과 테스트 사이 빈 줄이 하나다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `import java.util.*;` | 쓰는 타입마다 한 줄씩 |
| 기능 수정 PR 에서 파일 전체 Reformat | 바꾼 줄만 Reformat, 전체 정리는 별도 PR |
| IDE 기본 스타일에 맡김 | 저장소에 커밋한 스타일 파일을 따른다 |
| 이름 변경과 Reformat 을 한 커밋에 | 이름 변경 커밋, Reformat 커밋을 나눈다 |
| AI 가 파일을 다시 쓰며 들여쓰기·줄바꿈을 바꿈 | 편집한 줄만 바꾸도록 지시 |
| 의존 목록 전체를 알파벳으로(모듈과 라이브러리가 엇갈림) | 라이브러리 묶음 다음 모듈 묶음 - 이 모듈이 어느 모듈을 무는지 몇 줄로 보인다 |

## 다른 언어에서

- 원칙(저장소에 커밋한 스타일을 따르고, 바꾼 줄만 정리, 와일드카드 import 금지)은 같다. 도구만 다르다.
- TypeScript: `.editorconfig` + Prettier·ESLint 설정을 커밋. `import * as x` 를 남발하지 않는다.
- Python: `pyproject.toml` 의 ruff/black 설정을 커밋. `from x import *` 금지.
- 저장소 전체 포맷은 따로 한 번, 별도 커밋으로.
- 의존 목록: npm, pnpm 은 `package.json` 을 도구가 알파벳으로 쓰니 그대로 둔다. Python `pyproject.toml` 은 운영 목록과 개발 그룹을 나누고 각 안을 알파벳 순으로. Maven 은 같은 자리 원칙으로 `<dependencies>` 안을 라이브러리, 모듈, `test` 범위 순으로.

## Consequences

### 긍정적
- diff 가 작고 리뷰가 기능에 집중된다.
- 사람과 AI 가 같은 모양으로 쓴다.

### 부정적
- 지금 코드 모양과 스타일이 다른 곳은 한동안 섞여 있다(바꾼 줄부터 맞춰 간다).
- 빌드 강제가 없으므로 리뷰·정적 분석으로 지켜야 한다.

## References

- SonarQube 규칙 S2208(와일드카드 import)
- 예시: [examples/007-code-style](examples/007-code-style/README.md) - 바른 모양(good)과 어긋난 모양(bad)
