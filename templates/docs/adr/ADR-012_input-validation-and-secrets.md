# ADR-012: 입력은 경계에서 검증하고, 내부 정보와 비밀값은 밖으로 내지 않는다

## Status

Accepted

## Date

2026-10-30

## Intent

틀린 입력이 안쪽까지 들어오지 않게 하고, 오류 응답과 코드 저장소가 공격자에게 정보를 주지 않게 한다.

## Context

- 기존 상태: 요청 값 검사가 서비스 곳곳에 흩어져 있다. SQL 을 문자열로 이어 만든다. 오류 응답에 스택이나 예외 메시지를 그대로 담는다. 비밀값이 설정 파일이나 코드에 들어 있다.
- 문제점: 같은 검사가 중복되고 빠지는 자리가 생긴다. SQL 주입, 내부 구조 노출, 비밀값 유출이 생긴다.
- 제약 조건: 비밀값을 어디서 받는지(환경변수, 비밀 관리 서비스)는 배포 환경마다 다르다.

## Decision

**검증은 경계에서, 오류는 정해진 모양으로, 비밀값은 바깥에서 받는다.**

- 입력 검증:
  - 요청은 경계(컨트롤러, 메시지 수신부)에서 요청 타입의 선언적 검증(`@Valid` + 제약 애너테이션)으로 먼저 거른다. 실패는 400 계열과 어느 칸이 왜 틀렸는지로 돌려준다.
  - 업무 규칙 위반은 업무 사유 예외로(ADR-005). 값 객체 생성자 검사는 프로그래밍 오류 방어용이다(ADR-001).
  - 외부 시스템이 준 값도 입력이다. 어댑터에서 검사한다.
- 주입 방지: SQL·쿼리는 매개변수 바인딩으로만 만든다. 사용자 값을 쿼리·명령·경로 문자열에 이어 붙이지 않는다.
- 오류 노출: 클라이언트에 스택, 예외 클래스 이름, SQL, 내부 메시지를 보이지 않는다. 오류 응답은 한 모양(코드, 사람이 읽는 메시지, 추적용 ID)으로 통일하고, 자세한 것은 서버 로그에 남긴다(ADR-010).
- 비밀값:
  - 코드, 저장소에 커밋되는 설정 파일, 테스트 고정값에 두지 않는다. 배포 환경(환경변수, 비밀 관리 서비스)에서 받는다.
  - 받는 자리는 설정 객체 한 곳이다(ADR-002). 코드 곳곳에서 `System.getenv` 를 부르지 않는다.
  - 필수 비밀값이 없으면 기동 때 실패한다.

### 체크리스트

- [ ] 요청 타입에 선언적 검증이 있고 컨트롤러가 `@Valid` 로 받는다
- [ ] 쿼리에 사용자 값을 문자열로 이어 붙이지 않는다
- [ ] 오류 응답에 스택·예외 메시지·SQL 이 없다
- [ ] 코드·커밋되는 설정에 비밀번호·토큰·키가 없다
- [ ] `System.getenv` 가 설정 객체 밖에 없다
- [ ] 필수 비밀값이 없으면 기동이 실패한다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| 서비스마다 `if (req.email() == null) throw ...` | 요청 타입의 `@NotBlank @Email` + `@Valid` |
| `"select * from orders where id = " + id` | 매개변수 바인딩 |
| 오류 응답 `{"error": e.getMessage(), "trace": ...}` | `{"code": "ORDER_NOT_FOUND", "message": "...", "traceId": "..."}` |
| `application.yml` 에 `password: admin123` 커밋 | `${DB_PASSWORD}` + 배포 환경에서 주입 |
| `String key = System.getenv("API_KEY");` 를 여러 클래스에서 | 설정 record 한 곳에서 받고 검증 |

## 다른 언어에서

- 원칙(경계에서 검증, 바인딩, 오류에 내부 정보 금지, 비밀값은 설정 한 곳)은 같다.
- TypeScript: 요청은 스키마(zod, class-validator)로 검증, ORM·쿼리 빌더의 매개변수 바인딩.
- Python: pydantic 모델로 요청 검증, DB 드라이버·ORM 의 매개변수 바인딩.

## Consequences

### 긍정적
- 틀린 입력이 경계에서 걸려 안쪽 코드가 단순해진다.
- 주입, 정보 노출, 비밀값 유출 경로가 줄어든다.

### 부정적
- 요청 타입과 검증 애너테이션이 늘어난다.
- 로컬 실행에도 비밀값을 주입하는 준비가 필요하다.

## References

- 출처: [affaan-m/everything-claude-code rules/java/security.md](https://github.com/affaan-m/everything-claude-code/blob/main/rules/java/security.md), [affaan-m/everything-claude-code java-reviewer.md](https://github.com/affaan-m/everything-claude-code/blob/main/agents/java-reviewer.md), [github/awesome-copilot springboot.instructions.md](https://github.com/github/awesome-copilot/blob/main/instructions/springboot.instructions.md)
- ADR-001, ADR-002, ADR-005, ADR-010
