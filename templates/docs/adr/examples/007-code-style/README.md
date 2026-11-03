# ADR-007 예시: 와일드카드 import 없이, 의존 목록은 자리와 알파벳 순으로

## 무엇을 보여 주나

코드 모양 가운데 파일로 보일 수 있는 세 가지다. 쓰는 타입마다 한 줄인 import, 와일드카드로 합치지 않게 하는 IntelliJ 프로젝트 스타일 파일, 자리(라이브러리 → 모듈, 운영 → 테스트)와 알파벳 순인 Gradle 의존 목록.
"Reformat 은 바꾼 줄에만" 은 파일이 아니라 diff 로만 보이니 아래 「무엇이 다른가」에 글로 적는다.

## 바른 모양

- `good/OrderLines.java` - `java.util.List`, `java.util.Map`, `java.util.stream.Collectors` 를 한 줄씩
- `good/Project.xml` - 저장소의 `.idea/codeStyles/Project.xml` 에 커밋하는 조각. 와일드카드 기준 999, 기본으로 와일드카드를 쓰는 패키지 목록 비움. 같은 폴더에 `codeStyleConfig.xml`(`USE_PER_PROJECT_SETTINGS` 를 `true`)도 커밋해야 IDE 가 이 스타일을 쓴다
- `good/build.gradle` - `implementation` 라이브러리, `implementation project(...)`, `runtimeOnly`, 빈 줄 하나, `testImplementation` 라이브러리, `testImplementation testFixtures(...)`, `testRuntimeOnly`. 같은 자리 안은 `group:artifact` 알파벳 순

## 어긋난 모양

- `bad/OrderLines.java` - `import java.util.*;`, `import java.util.stream.*;`
- `bad/build.gradle` - 라이브러리와 모듈이 엇갈리고, 테스트 의존이 운영 묶음에 끼고, 묶음 안에 빈 줄이 있고, 같은 자리 안이 알파벳 순이 아니다

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `import java.util.*;` | 쓰는 타입마다 한 줄 | `import .*\.\*;` 가 없다 |
| IDE 기본 스타일(기준 5, `java.awt.*` 기본 와일드카드) | `.idea/codeStyles/Project.xml` 커밋, 기준 999 | 저장소에 코드 스타일 파일이 커밋되어 있다 |
| 기능 PR 에서 파일 전체 Reformat | 바꾼 줄만 Reformat(IntelliJ "Reformat → VCS changed text") | diff 에 기능과 무관한 모양만 바뀐 줄이 없다 |
| 이름 변경과 Reformat 을 한 커밋에 | 이름 변경 커밋, Reformat 커밋을 나눈다 | 전체 Reformat 이 필요하면 별도 커밋으로 나뉘어 있다 |
| 라이브러리·모듈·테스트가 엇갈린 의존 목록 | 자리 → 종류 → 알파벳, 운영과 테스트 사이 빈 줄 하나 | 빌드 파일 의존 목록이 자리(라이브러리 → 모듈, 운영 → 테스트)와 알파벳 순이고, 운영과 테스트 사이 빈 줄이 하나다 |

의존 목록 순서를 맞추는 커밋은 줄 순서만 바꾸고 의존을 더하거나 빼지 않는다.

## 기계 검사

- `checks/checkstyle-adr.xml` 의 `AvoidStarImport` 가 와일드카드 import 를 막는다(`bad/OrderLines.java` 가 걸린다).
- 스타일 파일 커밋, 바꾼 줄만 Reformat, 의존 목록 순서는 리뷰가 체크리스트로 본다.

## 공개 참고

- spring-boot [`.idea/codeStyles/Project.xml`](https://github.com/spring-projects/spring-boot/blob/main/.idea/codeStyles/Project.xml) - 저장소에 코드 스타일 파일을 커밋한 모양. 다른 점: 와일드카드로 합치는 기준값이 500 이다(이 ADR 은 999). 들여쓰기에 탭을 쓴다.
