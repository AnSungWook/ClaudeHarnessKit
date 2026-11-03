# ADR-003 예시: 설정은 record 한 자리에서 받고, 기본 파일이 운영 값이다

## 무엇을 보여 주나

결제사 클라이언트가 쓰는 설정(주소, API 키, 타임아웃)을 받는 자리다. 바른 모양은 `@ConfigurationProperties` record 하나가 키 깊이대로 받고 기동 때 빈 값과 범위 밖 값을 막으며, 기본 파일이 운영 값을 들고 `local` 파일이 바깥 주소를 덮는다.
어긋난 모양은 `@Value` 로 칸마다 받고, 기본값을 코드와 `${X:기본}` 에 숨기고, 프로파일 이름으로 코드를 가른다.
시각(`Clock`) 갈래는 [ADR-008 예시](../008-constructor-injection/README.md)의 서비스와 고정 시계 테스트가 보인다.

## 바른 모양

- `good/OrderProperties.java` - `order.payment.*` 를 같은 깊이의 중첩 record(`Payment`, `Payment.Timeout`)로 받는다. API 키는 빈 값과 풀리지 않은 `${` 글자를 막고, `toString` 에서 가린다. 타임아웃은 1ms 이상, int 밀리초(`Integer.MAX_VALUE` ms) 이하만 받고 어기면 그 설정 키 이름을 담아 기동을 막는다 - 요청 팩토리가 밀리초로 적용해 1ms 미만은 0(무한 대기)이 되기 때문이다
- `good/OrderPaymentClientConfig.java` - 설정 record 를 읽는 자리는 설정 층 하나다. 기동 때 설정 요약을 한 줄로 남긴다(키는 가려진다)
- `good/application.yaml` - 기본 파일이 운영 값. 기본 프로파일 `local`, 비밀값만 기본값 없는 `${ORDER_PAYMENT_API_KEY}`
- `good/application-local.yaml` - 바깥 주소만 로컬 값으로 덮는다

## 어긋난 모양

- `bad/OrderPaymentClientConfig.java` - `@Value("${...:기본}")` 를 칸마다, 비밀값에 빈 기본값, `environment.acceptsProfiles("local")` 로 주소를 바꾼다
- `bad/application.yaml` - 일반 값의 `${X:기본}`, 하이픈으로 되풀이한 형제 키(`payment-connect-timeout`, `payment-read-timeout`), 한 파일을 `---` 와 `on-profile` 로 나눔, 기본 프로파일 없음

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 체크리스트 |
|---|---|---|
| `@Value("${order.payment-base-url:...}") String baseUrl` | `OrderProperties` record 생성자 바인딩 | `@Value(` 가 없다 / 설정 바인딩 클래스가 record(생성자 바인딩)이고 setter 가 없다 |
| 기본값이 `@Value` 와 yaml 두 자리 | 값은 yaml 한 자리, 검사는 record 한 자리 | 설정 기본값이 한 곳에만 있다 |
| `payment-connect-timeout`, `payment-read-timeout` | `payment.timeout.connect`, `payment.timeout.read` 와 중첩 record `Timeout` | 형제 키가 같은 앞 낱말이나 뒤 낱말을 하이픈으로 되풀이하지 않는다 |
| `${PAYMENT_BASE_URL:https://...}`, `${ORDER_PAYMENT_API_KEY:}` | 일반 값은 값 그대로, 비밀값은 `${ORDER_PAYMENT_API_KEY}` + 기동 검사 | yaml 에 `${X:기본}` 모양이 없고, 비밀값 `${X}` 는 빈 값과 `${` 글자를 기동 때 막는다 |
| 타임아웃 범위 검사 없음 | `Timeout` 이 1ms 미만과 int 밀리초를 넘는 값을 막는다 | 범위 밖 설정이 기동 때 실패한다 |
| `---` + `on-profile: local` | `application-local.yaml` 파일 하나 | 한 파일 안에 `---` + `on-profile` 이 없고, 운영 전용 `-prd` 파일이 없다 |
| 기본 프로파일 없음 | `spring.profiles.active: local`, local 파일이 바깥 주소를 덮음 | 기본 프로파일이 `local` 이고 `local`, `test` 환경 파일이 바깥 주소를 모두 덮는다 |
| `environment.acceptsProfiles(Profiles.of("local"))` | 환경 파일의 값으로 가름 | 코드에 `getActiveProfiles()`, `acceptsProfiles` 분기가 없다 |

이 예시는 설정 갈래만 다룬다. 코드값 enum(`UseYn`)과 키 조립 한 자리(`StorageKeys`)는 ADR 본문의 안티패턴 표를 본다.

## 기계 검사

- `checks/AdrArchitectureTest.java` 의 `noValueOnFields`, `noValueOnParameters` 가 `@Value` 를 막는다(`bad/OrderPaymentClientConfig.java` 의 매개변수가 걸린다).
- 같은 파일의 `noSystemClockReads` 가 `Clock` 없는 `now()` 와 `System.currentTimeMillis()` 를 막는다.
- yaml 의 모양(`${X:기본}`, 키 깊이, `---` + `on-profile`)과 `acceptsProfiles` 분기는 리뷰가 체크리스트로 본다.

## 공개 참고

- spring-boot 문서 [constructorbinding `MyProperties`](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/main/java/org/springframework/boot/docs/features/externalconfig/typesafeconfigurationproperties/constructorbinding/MyProperties.java) - 생성자 바인딩, 키 깊이를 따른 중첩. 다른 점: record 가 아니다(final 필드와 getter), 비밀번호를 일반 칸으로 받는다(이 ADR 은 비밀값을 기본값 없는 환경변수로, 빈 값을 기동 때 막는다).
