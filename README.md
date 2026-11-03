# adr-starter

새 프로젝트를 시작할 때 까는 **공통 코드 설계 결정(ADR)** 모음이다.
ADR 은 프로젝트 초기에 잡혀 있어야 코드 품질이 잡힌다. 이미 굳은 코드에 나중에 걸면 잘 안 바뀐다.

## 들어 있는 것

| 자리 | 내용 |
|---|---|
| `templates/docs/adr/` | 공통 ADR-001 ~ ADR-023, 템플릿 ADR-000, 목록 `README.md` |
| `templates/docs/adr/examples/` | ADR 예시 코드(ADR-001 배치 실행 흐름 등) |
| `templates/docs/adr/checks/` | 기계로 판정할 수 있는 ADR 을 빌드에서 막는 견본(ArchUnit, Checkstyle, commitlint) |
| `templates/CLAUDE.md.template` | 세션이 ADR-001 부터 읽게 하는 프로젝트 `CLAUDE.md` 뼈대 |
| `setup.sh` | `docs/adr/`(예시, 검사 견본 포함)와 `CLAUDE.md` 템플릿을 프로젝트에 까는 스크립트 |

**ADR-001(사람이 따라 읽을 수 있게)이 나머지의 전제다.** 코드를 누가 짜든(사람이든 AI 든) 결국 사람이 읽고 확인한다.

공통 ADR 은 어느 프로젝트에나 걸리는 **설계 원칙과, 어디서나 쓸 기본 구현 선택**(설정 배치, 패키지 배치, 오류 응답 본문, 커밋 모양 등)을 든다. 그 밖의 프로젝트 구현 규격(DB 접근, 오류 본문 밖의 API 계약, 관측 등)이나 기본 선택을 바꾸는 결정은 공통 ADR 을 적용한 뒤 그 프로젝트에서 정해 `ADR-024` 부터 쓴다. 다 지키기 어려우면 `templates/docs/adr/README.md` 의 「지키는 순서」 1단계부터 지킨다.

## 까는 법

프로젝트 루트에서 Git Bash(또는 bash)로 실행한다.

```bash
bash /path/to/adr-starter/setup.sh
```

- `docs/adr/` 가 없으면 ADR 과 예시를 통째로 복사한다. 있으면 건드리지 않는다.
- `CLAUDE.md` 가 없으면 템플릿을 둔다. 있으면 템플릿의 「설계 결정(ADR)」 절을 손으로 옮겨 넣는다.

## ADR 을 고칠 때

- 공통 ADR 은 이 저장소의 `templates/docs/adr/` 에서 고치고 PR 로 넣는다. 원칙은 언어 무관, 예시는 Java/Spring 이다.
- 목록과 "프로젝트 결정은 ADR-024 부터" 문장은 `templates/docs/adr/README.md` 가 든다. 공통 ADR 을 더하면 둘 다 고친다. `CLAUDE.md` 템플릿은 번호를 박지 않고 이 문장을 가리킨다.
- 이미 깐 프로젝트의 `docs/adr/` 는 저절로 따라오지 않는다. 2026-11-03 에 번호를 한 칸씩 밀었으니 그 전에 깐 프로젝트와는 번호 대신 제목으로 부른다.

## 걷은 것 (2026-11-03)

에이전트 팀(`/task`, `/jira-task` 워크플로, 에이전트 8개), 스킬(`/server`, `/db`, `/e2e-test`, `/analyze-spec`, `/propagate-convention` 등), 훅, 명령, `config.yaml`, npm 설치 진입점, `docs/standards` 템플릿, 워크플로 설계 문서를 걷었다.
실제 작업 흐름은 따로 쓰고 있어 공통 킷에는 ADR 만 남긴다. 필요하면 git 이력(`92e7189` 이전)에서 되살린다.
