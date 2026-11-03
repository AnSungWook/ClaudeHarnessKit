# ADR-013 예시: 입력은 경계에서 검증하고, 내부 정보와 비밀값은 밖으로 내지 않는다

[ADR-013](../../ADR-013_input-validation-and-secrets.md) 을 회원 가입 창구, 회원 조회 SQL, 결제사 설정에 적용한 예다.

## 무엇을 보여 주나

요청은 요청 record 의 제약 어노테이션과 `@Valid` 로 경계에서 거르고, SQL 은 매개변수로만 묶는다.
비밀값은 설정 record 한 자리에서 기본값 없이 받고, 비었거나 풀리지 않은 `${...}` 이면 기동을 막는다.

## 바른 모양

- `good/MemberSignUpRequest.java` - 칸마다 `@NotBlank`, `@Email`, `@Size`. `toString` 이 비밀번호를 가린다(ADR-011).
- `good/MemberSignUpController.java` - `@Valid @RequestBody` 로 받고, 바꾸고, 서비스를 한 번 부르고, 바꾼다. 검증 실패와 오류 본문은 공통 처리기가 낸다(ADR-023).
- `good/JdbcMemberRepository.java` - `:email` 매개변수 바인딩. DB 예외는 원인을 단 사유 예외로 감싸고, 개인정보인 이메일은 문구에 싣지 않는다(ADR-006, ADR-011).
- `good/PaymentGatewayProperties.java` - 비밀값 `apiKey` 를 받는 한 자리. `@Validated` + `@NotBlank` 로 빈 값을, 생성자에서 `${` 글자를 막고, `toString` 이 키를 가린다. 타임아웃은 ADR-003 의 키 깊이대로 `timeout.connect`, `timeout.read` 이고, 1ms 이상 int 밀리초(`Integer.MAX_VALUE` ms) 이하가 아니면 그 키 이름을 담아 기동을 막는다.

설정 파일은 이렇게 둔다(ADR-003). 비밀값만 환경변수이고 기본값이 없다.

```yaml
payment-gateway:
  base-url: https://pg.example.com
  api-key: ${PAYMENT_GATEWAY_API_KEY}
  timeout:
    connect: 2s
    read: 5s
```

## 어긋난 모양

- `bad/MemberSignUpController.java` - `@Valid` 없이 받고 컨트롤러 `if` 로 검사, 예외 문구와 클래스 이름을 응답 본문에 싣는다.
- `bad/JdbcMemberRepository.java` - 사용자 값을 SQL 문자열에 이어 붙인다.
- `bad/PaymentGatewayAdapter.java` - 코드에 박은 키, 어댑터 안의 `System.getenv`, 비밀값이 없어도 기본값으로 도는 기동.

`bad/MemberSignUpController.java` 의 `catch` 는 ADR-006(중간에서 잡기), ADR-023(처리기 한 자리)도 어긴다. 표시는 이 ADR 의 줄에만 달았다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| `@RequestBody MemberSignUpRequest request` + `if (request.email() == null ...)` | 요청 record 의 `@NotBlank @Email` + `@Valid @RequestBody` | 요청 타입에 선언적 검증이 있고 컨트롤러가 `@Valid` 로 받는다 |
| `"... where email = '" + email + "'"` | `where email = :email` + `.param("email", email)` | 쿼리에 사용자 값을 문자열로 이어 붙이지 않는다 |
| `Map.of("error", e.getMessage(), "type", e.getClass().getName())` | 공통 처리기의 고정 모양 본문(`errorCode`, `errorMessage`, `traceId`) | 오류 응답에 스택·예외 메시지·SQL 이 없다 |
| `FALLBACK_API_KEY = "pg-live-key-0000"` | `api-key: ${PAYMENT_GATEWAY_API_KEY}`, 배포 환경에서 주입 | 코드·커밋되는 설정에 비밀번호·토큰·키가 없다 |
| 어댑터의 `System.getenv("PAYMENT_GATEWAY_API_KEY")` | `PaymentGatewayProperties` 한 자리에서 받고 어댑터는 그 값을 주입받는다 | `System.getenv` 가 설정 객체 밖에 없다 |
| 키가 없으면 기본값으로 계속 | `@NotBlank` 와 `${` 검사로 기동 실패 | 필수 비밀값이 없으면 기동이 실패한다 |

- `MemberSignUpService`, `MemberSignUpCommand`, `MemberSignUpResult`, `MemberSignUpResponse`, `CommonResponse`, `Member`, `MemberRepository`, `MemberStorageUnavailableException`(`UnavailableException` 형제), `PaymentGatewayEmptyResponseException` 은 이름으로만 쓴다.
- 이메일 중복 같은 업무 규칙은 서비스가 도메인 검증을 불러 사유 예외(`ConflictException` 형제)로 낸다(ADR-006). 이 예시는 형식 검증까지만 다룬다.

## 기계 검사

이 ADR 을 막는 기계 검사는 없다. 리뷰가 체크리스트로 본다. (`@Value` 금지는 ADR-003 규칙으로 [`checks/AdrArchitectureTest.java`](../../checks/README.md) 가 막는다.)

## 공개 참고

- [spring-boot 문서 예제 `validation/MyProperties`](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/main/java/org/springframework/boot/docs/features/externalconfig/typesafeconfigurationproperties/validation/MyProperties.java) - `@Validated` 로 필수 설정이 없으면 기동이 실패하는 모양.
  - 우리 ADR 과 다른 점: setter 가 있는 가변 클래스다(우리는 record, ADR-003·009). 빈 값과 풀리지 않은 `${...}` 글자는 막지 않는다.
