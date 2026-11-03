# ADR-019 예시: 인증은 한 자리, 보안 기본값은 닫힌 쪽

[ADR-019](../../ADR-019_auth-boundary-and-security-defaults.md) 을 내부 시스템만 부르는 API 에 적용한 예다. 로그인과 역할이 없어 무거운 인증 프레임워크 대신 공유 비밀 헤더 하나로 막는다.

## 무엇을 보여 주나

인증은 `auth` 기능의 필터 하나가 건다. 열어 둘 경로(`/actuator/health`, `/actuator/info`)만 허용 목록에 적고 나머지는 전부 인증한다. 컨트롤러에는 인증 코드가 없다.
CORS 는 목록이 비면 닫힌 채, Actuator 는 `health`, `info`, `prometheus` 만, 업로드는 크기 상한, 회원이 준 URL 받기는 사설 주소로 못 가게 한다.

## 바른 모양

- [`good/InternalTokenAuthenticationFilter.java`](good/InternalTokenAuthenticationFilter.java) - 인증 한 자리(`auth/adapter/in/web`). 헤더 토큰을 끝까지 견주는 비교로 확인하고, 틀리면 공통 오류 본문(ADR-023)으로 401 을 쓴다.
  - 경로 판정은 context-path 를 뺀 경로로, "막을 것" 이 아니라 "열어 둘 것" 목록으로 한다. 목록과 글자가 하나라도 다르면(새 경로, `;jsessionid` 같은 꼬리) 인증 쪽으로 떨어져 닫힌 채다.
  - CORS 사전 요청(OPTIONS)은 토큰 없이 오므로 인증 전에 통과시킨다. MVC 가 CORS 판정만 하고 컨트롤러는 부르지 않는다.
- [`good/AuthProperties.java`](good/AuthProperties.java) - 토큰과 허용 origin 목록. 풀리지 않은 `${...}` 와 `*` origin 이면 기동이 멈춘다(ADR-003).
- [`good/AuthConfig.java`](good/AuthConfig.java) - 필터 빈(Spring Boot 가 서블릿 필터로 등록한다)과 CORS. origin 은 목록으로만 연다.
- [`good/ProfileImageClientConfig.java`](good/ProfileImageClientConfig.java) - 받는 곳을 정할 수 없는 URL(회원 프로필 이미지)을 받는 클라이언트. `InetAddressFilter.externalAddresses()` 로 사설, 루프백, 링크 로컬 주소를 막는다. 클라이언트 모양과 타임아웃 범위 검사(`ProfileImageClientProperties`)는 [018 예시](../018-outbound-http-clients/README.md)의 `PaymentGatewayProperties` 와 같다.
- [`good/application.yaml`](good/application.yaml) - Actuator 노출 목록, 업로드 상한, 기본값 없는 토큰 env.

남의 자원은 403 이 아니라 404 로 답한다. 주인 조건을 조회에 넣어 "없음" 하나로 끝낸다.

```java
@Transactional(readOnly = true)
public OrderResult getMemberOrder(long memberId, long orderId) {
    return orders.findByIdAndMemberId(orderId, memberId)
        .map(OrderResult::of)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
}
```

## 어긋난 모양

- [`bad/OrderController.java`](bad/OrderController.java) - 컨트롤러가 직접 토큰을 견주고, 토큰이 코드에 박혀 있고, `@CrossOrigin(origins = "*")` 로 연다. 401 은 빈 본문이다.
- [`bad/MemberOrderQueryService.java`](bad/MemberOrderQueryService.java) - 남의 주문에 403 을 내 그 주문이 있다는 것을 알려 준다.
- [`bad/application.yaml`](bad/application.yaml) - Actuator `include: "*"`, 업로드 상한을 `-1` 로 풀어 버림, 토큰에 기본값.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| 컨트롤러마다 `if (!INTERNAL_TOKEN.equals(token))` | `InternalTokenAuthenticationFilter` 한 자리, 여는 경로만 허용 목록 | 인증 필터, 설정이 한 자리에만 있다 |
| `@CrossOrigin(origins = "*")` | `AuthConfig.addCorsMappings` 에 명시한 origin 목록, `*` 는 기동 때 막음 | CORS 에 `*` 가 없다 |
| `exposure.include: "*"` | `include: health,info,prometheus` | Actuator 노출이 `health`, `info`, 메트릭뿐이다 |
| `max-file-size: -1`(상한 풂) | 업무에 맞춘 `max-file-size`, `max-request-size` 를 적는다(Boot 기본값 1MB, 10MB 에 기대지 않고 정한 값을 보이게) | 업로드에 확장자 허용 목록과 크기 상한이 있다 |
| 회원 URL 을 아무 주소로나 받음 | `withInetAddressFilter(InetAddressFilter.externalAddresses())` | 바깥 파일 받기가 리다이렉트를 무조건 따라가지 않는다 |
| 남의 주문에 403 | `findByIdAndMemberId` 가 비면 `OrderNotFoundException`(404) | 남의 자원 접근이 404 다 |
| `${INTERNAL_TOKEN:local-token}`, 코드 상수 토큰 | `${INTERNAL_TOKEN}` + 풀리지 않은 글자 막기 | (ADR-003, ADR-013 체크리스트) |

- 확장자 허용 목록과 압축 파일의 푼 크기, 항목 수 상한은 업로드를 받는 어댑터가 든다. 이 예시는 크기 상한까지만 보인다.
- 받는 곳이 정해진 자리(우리 파일 서버, 정해진 파트너)는 사설 주소 막기 대신 도메인 허용 목록으로 리다이렉트를 따라간다. 이 예시는 정해지지 않은 자리만 보인다.
- 망 구성을 확인하기 전이라 막지 않고 경고 로그만 둘 때는 그 자리에 이유 주석을 달고 남은 일로 기록한다(ADR-019 의 예외).

## 기계 검사

리뷰가 체크리스트로 본다. [`checks/`](../../checks/README.md) 견본에는 이 ADR 을 막는 규칙이 없다.

## 공개 참고

- spring-boot 문서 [`inetaddressfiltering/MyService`](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/main/java/org/springframework/boot/docs/io/restclient/globalconfiguration/inetaddressfiltering/MyService.java) - 사설 주소로 가는 요청을 클라이언트 설정에서 막는다. `ProfileImageClientConfig` 가 같은 API 를 쓴다(`InetAddressFilter` 는 Spring Boot 4.1 부터 있다).
- spring-security-samples [`SecurityConfiguration`](https://github.com/spring-projects/spring-security-samples/blob/main/servlet/spring-boot/java/hello-security-explicit/src/main/java/example/SecurityConfiguration.java) - 보안 설정을 한 자리에 두고 기본값을 닫는다. 사람이 로그인하는 창구라면 이 모양으로 Spring Security 를 세운다.
  - 우리 ADR 과 다른 점: 비밀번호가 코드에 박혀 있다(ADR-003, ADR-013).
