# ADR-002: 상수와 설정값은 한 자리에서 만들고 해석한다

## Status

Accepted

## Date

2026-10-30

## Intent

같은 뜻의 값(키, 코드값, 운영 설정, 기본값)이 만들어지는 자리와 해석되는 자리를 하나로 묶어, 한쪽만 고쳐 어긋나는 일을 없앤다.

## Context

- 기존 상태: 저장 경로·키 문자열을 여러 클래스가 각자 조립한다. `"Y"`/`"N"` 같은 코드값을 문자열로 비교한다. 설정을 `@Value` 로 필드마다 흩뿌리고 기본값을 여러 곳에 둔다.
- 문제점: 만드는 쪽과 읽는 쪽 중 한쪽만 바뀌어도 컴파일은 통과하고 조용히 못 찾는다. 기본값이 두 군데면 어느 값으로 떴는지 모른다.
- 제약 조건: 외부와 주고받는 JSON 의 문자열 칸 모양은 바꾸지 않는다(비교만 enum 으로).

## Decision

**키·코드값·운영 설정·시각은 각각 정본이 한 자리다.**

- 저장 경로·키는 한 클래스(예: `StorageKeys`)가 만들고 해석한다. 해석이 필요한 키는 값 객체로.
- 코드값은 enum. 사용 여부 `"Y"`/`"N"` 같은 문자열은 그 enum(예: `UseYn`) 밖에서 쓰지 않는다. 외부 JSON 칸은 문자열 그대로 두고 비교만 enum 으로.
- 운영값은 `@ConfigurationProperties` record(생성자 바인딩, 또는 언어별 타입 있는 불변 설정 객체)에, 기본값은 한 곳에. `@Value` 를 쓰지 않는다. 설정 바인딩 클래스에도 setter 를 두지 않는다(ADR-008).
- 범위 밖 설정값은 기동 때 막는다(검증 실패로 앱이 안 뜨게). 기동 때 설정 요약을 로그 한 덩이로 남긴다.
- 우리 설정 키는 깊이로 나눈다. 형제 키가 같은 낱말을 하이픈으로 붙여 되풀이하면 그 낱말을 부모 키로 한 단계 올리고, 갈리는 부분만 자식 키로 둔다.
  - 겹치는 낱말이 뒤에 오든(`email-template`, `sms-template` → `template.email`, `template.sms`) 앞에 오든(`retry-count`, `retry-delay` → `retry.count`, `retry.delay`) 같다.
  - 부모 키는 자식들이 무엇인지 말하는 이름이다. 형제끼리 겹치지 않는 여러 낱말 키는 하이픈 그대로 둔다(`max-size`).
  - 프레임워크 키(`spring.*`, `server.*`, `management.*`, `logging.*`)는 그 이름 그대로 쓴다.
  - 바인딩하는 설정 record 도 같은 깊이로 중첩 record 를 둔다(`template.email` 이면 `TemplateProperties` 안의 `email` 칸). 키를 납작하게 늘어놓으면 묶음이 이름 속에 숨어 그 묶음을 단위로 넘기거나 검증할 수 없다.
- 설정 파일은 이렇게 놓는다.
  - 기본 파일(`application.yaml`, 모듈 조각 `application-<모듈>.yaml`)이 운영 값 겸 공통 값을 든다. 환경마다 다른 값만 환경 파일(`application-{env}.yaml`)이 덮는다. 운영 전용 파일은 따로 두지 않는다 - 기본 파일이 운영 값이다.
  - 한 파일 안을 `---` 와 `spring.config.activate.on-profile` 로 나누지 않는다. 환경마다 파일 하나다.
  - 기본 프로파일은 `local` 이다(`spring.profiles.active: local`). 배포는 환경변수(`SPRING_PROFILES_ACTIVE`)로 환경을 준다. 프로파일을 빠뜨린 기동이 운영 주소로 나가지 않게 하려는 것이다.
  - 프로파일 이름은 `local`, `dev`, `stg`, `prd`, `test` 처럼 저장소에서 하나로 정해 쓴다. 개인 덮어쓰기 파일(`application-local-mine.yaml` 등)은 gitignore 한다.
  - `local` 과 `test` 환경 파일은 바깥으로 나가는 주소(DB, 바깥 API, 버킷, 파일 서버)를 모두 덮는다. 안 덮으면 기본 파일의 운영 값으로 나간다.
- 비밀값(비밀번호, 키, 토큰)만 환경변수 `${ENV}` 로 받고 기본값을 두지 않는다(ADR-012). 값이 비었거나 `${` 로 시작하는 글자가 그대로 남았으면 기동을 막는다 - Spring 은 환경변수가 없으면 `${X}` 글자를 그대로 칸에 넣기 때문에, 기본값만 걷으면 그 글자가 비밀값으로 쓰인다.
- 호스트, 포트, 버킷, 도메인 같은 일반 값은 yaml 에 값으로 적는다. `${X:기본}` 모양은 쓰지 않는다 - 환경마다 무엇이 다른지가 저장소에서 보이게 한다. 저장소 설정에 절대 경로(`/Users/...`, `C:\...`)를 쓰지 않는다.
- 코드에서 프로파일 이름으로 갈라 동작하지 않는다(`Environment.getActiveProfiles()`, `acceptsProfiles`). 갈라야 하면 `@Profile` 이나 환경 파일의 값으로 가른다.
- 같은 뜻의 설정은 정본이 하나. 프레임워크가 이미 가진 키와 같은 뜻의 자체 프로퍼티를 만들지 않는다.
- 현재 시각은 주입한 `Clock` 에서만 얻는다. `now()` 를 직접 부르지 않는다.
- 페이징, JSON 읽기·쓰기 같은 공통 도우미는 하나씩만 둔다.
- 결과(판정)를 바꾸는 값은 관리 화면 설정으로 빼지 말고 코드 안 상수에 이름을 붙인다 - 바뀐 이력이 코드 리뷰에 남게.

### 체크리스트

- [ ] `@Value(` 가 없다
- [ ] `"Y"`/`"N"`, 상태 코드 문자열 리터럴이 enum 밖에 없다
- [ ] 같은 키·경로를 두 클래스가 각자 조립하지 않는다
- [ ] 설정 기본값이 한 곳에만 있다
- [ ] `LocalDateTime.now()`, `Instant.now()` 등 직접 호출이 없다(테스트 제외)
- [ ] 범위 밖 설정이 기동 때 실패한다
- [ ] 형제 키가 같은 앞 낱말이나 뒤 낱말을 하이픈으로 되풀이하지 않는다(부모 키로 올렸다)
- [ ] 설정 바인딩 클래스가 record(생성자 바인딩)이고 setter 가 없다
- [ ] yaml 에 `${X:기본}` 모양이 없고, 비밀값 `${X}` 는 빈 값과 `${` 글자를 기동 때 막는다
- [ ] 한 파일 안에 `---` + `on-profile` 이 없고, 운영 전용 `-prd` 파일이 없다
- [ ] 기본 프로파일이 `local` 이고 `local`, `test` 환경 파일이 바깥 주소를 모두 덮는다
- [ ] 코드에 `getActiveProfiles()`, `acceptsProfiles` 분기가 없다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `@Value("${app.timeout:30}") int timeout` 를 여러 빈에 | `@ConfigurationProperties` record 하나에 기본값과 검증 |
| `if ("Y".equals(row.useYn()))` | `if (UseYn.of(row.useYn()).isUsed())` |
| 서비스마다 `"runs/" + id + "/result.json"` 조립 | `StorageKeys.result(id)` 와 그 해석 메서드 |
| 상수 클래스 하나에 문자열만 몰아넣기 | 만드는 규칙(조립)과 해석까지 가진 타입 |
| `Instant.now()` | 주입받은 `Clock` 의 `clock.instant()` |
| 판정 문턱을 관리자 설정 칸으로 노출 | 이름 붙은 코드 상수, 바꿀 때는 커밋으로 |
| `url: ${DB_URL:jdbc:postgresql://localhost:5432/app}` | 기본 파일에 운영 주소, `application-local.yaml` 에 로컬 주소를 값으로 |
| `password: ${DB_PASSWORD:}`(빈 기본값) | `password: ${DB_PASSWORD}` + 빈 값과 `${` 글자를 막는 기동 검사 |
| `if (env.acceptsProfiles("local")) skipAuth()` | 환경 파일의 `auth.enabled: false` 같은 키 |
| 설정 클래스에 `@Setter` | record 생성자 바인딩 |
| `retry-count`, `retry-delay`, `retry-backoff` 를 한 단계에 늘어놓기 | `retry.count`, `retry.delay`, `retry.backoff` 와 중첩 record `Retry` |

## 다른 언어에서

- 원칙(값의 정본은 한 자리, 설정은 타입 있는 객체로, 시각은 주입)은 같다. `@Value`, `@ConfigurationProperties` 는 Spring 의 예다.
- Kotlin: `@ConfigurationProperties` + `data class`, 또는 같은 원칙의 설정 객체.
- TypeScript(Node): 환경변수는 한 모듈에서 스키마(zod 등)로 읽고 검증해 타입 있는 객체로 내보낸다. 코드값은 `enum` 이나 `as const` 유니온. 시각은 `Clock` 같은 인터페이스를 주입.
- Python: pydantic `BaseSettings` 같은 설정 객체 하나, 코드값은 `Enum`, 시각은 주입한 함수·객체.

## Consequences

### 긍정적
- 값을 바꿀 때 한 자리만 고치고, 어긋나면 컴파일이나 기동에서 드러난다.
- 시각을 고정해 테스트가 결정적이 된다.

### 부정적
- 키·코드값 타입이 늘어난다.
- 기존 JavaBean 설정을 record 로 바꾸면 그 설정을 직렬화하던 곳의 모양이 바뀔 수 있다. 저장했다 다시 읽는 설정이면 칸 이름을 그대로 두고, 옛 저장본을 읽는 테스트를 먼저 만든 뒤 옮긴다.
- 환경 파일이 늘고, 비밀값은 로컬에서도 환경변수나 개인 덮어쓰기 파일로 넣어야 한다.

## References

- ADR-001 정적 팩토리와 책임
