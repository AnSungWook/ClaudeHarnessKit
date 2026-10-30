# docs/adr/ — Architecture Decision Records

프로젝트의 **의사결정 기록**을 보관하는 디렉토리.

## ADR이란?

ADR(Architecture Decision Record)은 **왜 이 결정을 했는가**를 기록하는 문서다.
`docs/standards/`가 "어떻게 해야 하는가(How)"를 정의한다면, ADR은 "왜 이렇게 결정했는가(Why)"를 기록한다.

| | docs/standards/ | docs/adr/ |
|---|---|---|
| 성격 | 규칙 (How) | 결정 기록 (Why) |
| 변경 빈도 | 거의 불변 | 계속 추가 |
| 예시 | "API는 항상 snake_case" | "ADR-007: snake_case 채택 (이유: 기존 시스템 호환)" |

## 기본 ADR (공통 코드 설계)

어느 프로젝트에서나 공통으로 쓰는 코드 설계 결정이다. 설치하면 같이 들어오고, 프로젝트 사정에 맞지 않으면 Status 를 `Deprecated` 로 바꾸거나 이유를 적고 고친다.
프로젝트마다 다른 결정(DB 접근 방식, 배포, 외부 연동 등)은 `ADR-010` 부터 이어서 쓴다.

| ADR | 결정 |
|---|---|
| [ADR-001](ADR-001_static-factory-and-ownership.md) | 값은 정적 팩토리로 만들고, 판단은 그 클래스가 다 가진다. 서비스는 흐름만. 잘못된 값은 만들어질 수 없다 |
| [ADR-002](ADR-002_constants-and-settings.md) | 상수와 설정값은 한 자리에서 만들고 해석한다(`@Value` 금지, 코드값 enum, 시각은 `Clock`) |
| [ADR-003](ADR-003_no-nested-types.md) | 중첩 타입은 업무가 실제로 겹칠 때만 쓴다 |
| [ADR-004](ADR-004_hexagonal-layers.md) | 헥사고날 - 층은 역할로 정하고, 의존은 안쪽으로만 흐른다. 바깥 계약 타입과 도메인을 섞지 않는다 |
| [ADR-005](ADR-005_domain-reason-exceptions.md) | 오류는 이름이 사유를 말하는 도메인별 예외로, 바깥 예외는 어댑터에서 감싼다. 실패를 숨기지 않는다 |
| [ADR-006](ADR-006_code-style.md) | 코드 모양은 커밋한 스타일을 따른다 - 와일드카드 import 없이, Reformat 은 바꾼 줄에만 |
| [ADR-007](ADR-007_constructor-injection.md) | 의존은 생성자로 받고 final 로 둔다 |
| [ADR-008](ADR-008_immutability-first.md) | 불변을 먼저 쓴다 - record, 불변 컬렉션, `Optional` 은 반환값에만 |
| [ADR-009](ADR-009_comments-explain-why.md) | 주석은 왜를 적는다 |

## 파일 네이밍

```
ADR-{NNN}_{slug}.md
```

예시:
- `ADR-001_static-factory-and-ownership.md`
- `ADR-007_jpa-by-default.md`

## 규칙 전파 연동

- `/propagate-convention` 스킬과 `convention-keeper` 에이전트는 ADR을 **규칙의 단일 원본(Single Source of Truth)**으로 사용한다
- 새 규칙을 에이전트/스킬에 전파하려면 **먼저 ADR을 생성**해야 한다
- ADR이 없는 규칙은 전파되지 않는다
