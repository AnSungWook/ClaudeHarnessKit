# docs/adr/ — Architecture Decision Records

프로젝트의 **의사결정 기록**을 보관하는 디렉토리.

## ADR이란?

ADR(Architecture Decision Record)은 **무엇을 정했고 왜 그렇게 정했는가**를 기록하는 문서다. 결정과 함께 리뷰에서 쓰는 체크리스트와 안티패턴을 든다.

## 기본 ADR (공통 코드 설계)

어느 프로젝트에서나 공통으로 쓰는 코드 설계 결정이다. 설치하면 같이 들어오고, 프로젝트 사정에 맞지 않으면 Status 를 `Deprecated` 로 바꾸거나 이유를 적고 고친다.
프로젝트마다 다른 결정(DB 접근 방식, 배포, 외부 연동 등)은 `ADR-024` 부터 이어서 쓴다.

- **ADR-001 이 나머지의 전제다.** 코드를 누가 짜든(사람이든 AI 든) 결국 사람이 읽고 확인한다. 따라 읽을 수 없는 코드는 다른 규칙을 지켰는지도 확인할 수 없으니, 리뷰와 리팩토링은 ADR-001 부터 본다.
- **2026-11-03 에 번호를 한 칸씩 밀었다.** 옛 ADR-001~022 가 지금 002~023 이다. 그 전에 설치한 프로젝트의 `docs/adr/` 는 옛 번호다. `setup.sh` 는 이미 있는 `docs/adr/` 를 건너뛰니 다시 돌려도 안 바뀐다 - 새 판과 직접 비교해 번호와 참조를 맞추기 전에는 프로젝트 사이에서 번호로 부르지 말고 제목으로 부른다.
- **원칙은 언어와 무관하고, 본문 예시는 Java/Spring 이다.** 각 ADR 의 「다른 언어에서」 절이 Kotlin, TypeScript, Python 에서 같은 원칙을 어떻게 적용하는지 적는다. `@Value`, `@Transactional` 같은 이름은 그 원칙의 Spring 쪽 예다.
- **아래 항목은 여러 선택지 중 하나를 고른 부분이다.** 근거는 각 ADR 에 있고, 프로젝트 규모나 팀 관례에 더 맞는 방식이 있으면 그 ADR 을 고쳐 바꾼다. 각 줄에서 "공통이다" 라고 적은 부분은 고른 방식과 상관없이 그대로 쓴다.
  - ADR-002: Mapper 대신 `of(원본)`
  - ADR-005: 헥사고날 층
  - ADR-003: 기본 설정 파일이 운영 값을 들고 환경 파일이 덮는 배치(반대로 기본 파일을 로컬 값으로 두는 팀도 있다). 비밀값만 env, 빈 값 막기, 프로파일 이름 분기 금지는 공통이다
  - ADR-016: `@Service` 이름 규칙(Spring 한정). `find`/`get` 동사와 용도 접미사는 공통이다
  - ADR-020: 제목 모양 `type(scope): subject`. 본문과 PR 에 왜와 검증을 적는 것은 공통이다
  - ADR-022: 어노테이션 순서(Spring, Lombok 쪽). public 밑에 그것이 부르는 private 을 두는 멤버 순서는 공통이다
  - ADR-006: 오류 코드를 도메인별 enum(code 는 `name()`)으로 두는 모양. 사유 예외, 공통 형제, 실패 사유 `코드: 메시지` 는 공통이다
  - ADR-007: 빌드 의존 목록의 자리 순서(Spring Initializr 순). 같은 자리 알파벳과 운영·테스트 구분은 공통이다
  - ADR-023: 오류 본문을 공통 응답 본문(`error{errorCode, errorMessage}`)으로 두는 모양(RFC 9457 `ProblemDetail` 을 쓰는 팀도 있다). 처리기 한 자리, 매핑 표 하나, 모르는 예외는 고정 문구는 공통이다

| ADR | 결정 |
|---|---|
| [ADR-001](ADR-001_readable-flow.md) | **먼저 본다.** 코드의 주인이 입력을 받은 자리부터 이름만 보고 각 단계가 무슨 일을 하는지 따라갈 수 있게 짠다 |
| [ADR-002](ADR-002_static-factory-and-ownership.md) | 값은 정적 팩토리로 만들고, 판단은 그 클래스가 다 가진다. 서비스는 흐름만. 잘못된 값은 만들어질 수 없다 |
| [ADR-003](ADR-003_constants-and-settings.md) | 상수와 설정값은 한 자리에서 만들고 해석한다(`@Value` 금지, 코드값 enum, 시각은 `Clock`). 기본 파일이 운영 값, 환경 파일이 덮고, 기본 프로파일 `local`, 비밀값만 기본값 없는 env |
| [ADR-004](ADR-004_no-nested-types.md) | 중첩 타입은 업무가 실제로 겹칠 때만 쓴다 |
| [ADR-005](ADR-005_hexagonal-layers.md) | 헥사고날 - 층은 역할로 정하고, 의존은 안쪽으로만 흐른다. 바깥 계약 타입과 도메인을 섞지 않는다 |
| [ADR-006](ADR-006_domain-reason-exceptions.md) | 오류는 이름이 사유를 말하는 도메인별 예외로, 바깥 예외는 어댑터에서 감싼다. 코드는 도메인별 ErrorCode enum, DB 실패 사유는 `코드: 메시지`. 실패를 숨기지 않는다 |
| [ADR-007](ADR-007_code-style.md) | 코드 모양은 커밋한 스타일을 따른다 - 와일드카드 import 없이, Reformat 은 바꾼 줄에만 |
| [ADR-008](ADR-008_constructor-injection.md) | 의존은 생성자로 받고 final 로 둔다 |
| [ADR-009](ADR-009_immutability-first.md) | 불변을 먼저 쓴다 - record, 불변 컬렉션, `Optional` 은 반환값에만 |
| [ADR-010](ADR-010_comments-explain-why.md) | 주석은 왜를 적는다 |
| [ADR-011](ADR-011_logging.md) | 로그는 수준을 뜻대로 쓰고, 예외는 경계에서 한 번만, 비밀값·개인정보는 남기지 않는다 |
| [ADR-012](ADR-012_test-scope-and-doubles.md) | 테스트는 단위·통합으로 나누고, 테스트 더블은 바깥 의존에만 쓴다 |
| [ADR-013](ADR-013_input-validation-and-secrets.md) | 입력은 경계에서 검증하고, 내부 정보와 비밀값은 밖으로 내지 않는다 |
| [ADR-014](ADR-014_transaction-boundary.md) | 트랜잭션은 응용 서비스에 두고, 그 안에서 바깥을 부르지 않는다 |
| [ADR-015](ADR-015_concurrency-basics.md) | 공유 상태를 두지 않고, 스레드는 정해진 풀에서, 중단을 삼키지 않는다. JVM 잠금은 JVM 안 상태에만 |
| [ADR-016](ADR-016_naming.md) | 이름은 뜻을 드러내고(Manager·Util 금지, 약어 금지), 업무 용어 하나로 통일한다. 값은 용도 접미사, 메서드는 층을 말하는 동사(`find` 는 Optional, `get` 은 없으면 예외) |
| [ADR-017](ADR-017_package-structure.md) | 패키지는 기능별로 나누고 그 안은 성격별 폴더(`domain`, `application/port`, `application/service`, `adapter/in`, `adapter/out`, `config`)로 고정한다. 리팩토링 범위에 패키지 정렬이 기본으로 들어간다 |
| [ADR-018](ADR-018_outbound-http-clients.md) | 바깥 HTTP 는 클라이언트 한 종류로 어댑터 안에서만 부르고, 타임아웃은 클라이언트마다 적으며, 재시도는 멱등 호출에만 건다 |
| [ADR-019](ADR-019_auth-boundary-and-security-defaults.md) | 인증은 한 자리가 들고, 보안 기본값은 닫힌 쪽이다(명시한 CORS, 좁힌 Actuator, 업로드 상한, 리다이렉트 제한) |
| [ADR-020](ADR-020_commit-and-pr-conventions.md) | 커밋 제목은 `type(scope): subject`, 본문과 PR 은 왜와 돌린 검증(안 한 검증 포함)을 든다 |
| [ADR-021](ADR-021_modern-language-idioms.md) | 같은 일은 최신 문법 한 모양으로 - 결과 순회는 스트림, `switch` 식은 default 없이 전부, 패턴 매칭, sealed 와 record |
| [ADR-022](ADR-022_annotation-and-member-order.md) | 정체 어노테이션은 선언 바로 위, 멤버는 public 하나 밑에 그것이 부르는 private |
| [ADR-023](ADR-023_http-error-boundary.md) | HTTP 오류는 처리기 한 자리가 매핑 표 하나로 상태와 본문을 정하고, 모르는 예외는 고정 문구만 낸다. 컨트롤러는 바꾸고, 한 번 부르고, 바꾼다 |

## 파일 네이밍

```
ADR-{NNN}_{slug}.md
```

예시:
- `ADR-002_static-factory-and-ownership.md`
- `ADR-024_jpa-by-default.md` (프로젝트 결정)

예시 코드는 `examples/` 에 두고 이름을 `ADR-` 로 시작하지 않는다(ADR 로 읽히지 않게).

## 쓰는 법

- 프로젝트 `CLAUDE.md` 가 이 폴더를 가리키고, 세션은 일하기 전에 ADR-001 부터 읽는다(`CLAUDE.md` 템플릿에 그 절이 있다).
- 리뷰는 바뀐 파일을 각 ADR 의 체크리스트와 대조하고, 어긴 것은 막는 지적으로 낸다.
- 새 규칙은 먼저 ADR 로 쓴다. ADR 이 없는 규칙은 리뷰 기준이 아니다.
