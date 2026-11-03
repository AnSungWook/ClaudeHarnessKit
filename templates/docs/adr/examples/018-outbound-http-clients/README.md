# ADR-018 예시: 바깥 HTTP 는 클라이언트 하나로, 타임아웃은 클라이언트마다, 재시도는 멱등 호출에만

[ADR-018](../../ADR-018_outbound-http-clients.md) 을 결제사(PG) 호출에 적용한 예다. Spring MVC 저장소라 클라이언트는 `RestClient` 하나다.

## 무엇을 보여 주나

결제 상태 조회(멱등)와 결제 승인(멱등 아님) 두 호출을 같은 클라이언트로 부른다.
타임아웃은 설정 객체에서 클라이언트 빈을 만들 때 넣고, 재시도는 상태 조회에만 건다. 바깥 예외는 어댑터가 실패 종류별 사유 예외로 바꾼다.

## 바른 모양

- [`good/PaymentGatewayProperties.java`](good/PaymentGatewayProperties.java) - 주소와 연결·읽기 타임아웃을 든 설정 record. 타임아웃이 없거나 1ms 미만이거나 int 밀리초(`Integer.MAX_VALUE` ms)를 넘으면 그 설정 키 이름을 담아 기동이 멈춘다.
- [`good/PaymentGatewayClientConfig.java`](good/PaymentGatewayClientConfig.java) - 이름 붙인 클라이언트 빈 `paymentGatewayRestClient`. `HttpClientSettings.withTimeouts(...)` 로 타임아웃이 눈에 보인다. `@EnableResilientMethods` 가 `@Retryable` 을 켠다.
- [`good/PaymentGatewayAdapter.java`](good/PaymentGatewayAdapter.java) - `payment/adapter/out/client` 의 포트 구현. 조회에만 `@Retryable(includes = PaymentGatewayUnavailableException.class)`. 429, 5xx, 연결 실패, 타임아웃은 `PaymentGatewayUnavailableException`(503 갈래), 그 밖의 4xx 는 `PaymentGatewayRejectedException` 으로 감싼다. 빈 본문이나 빠진 필수 칸은 `requireComplete`(본문은 줄였다)가 `PaymentGatewayInvalidResponseException` 으로 막는다(바깥 응답도 입력이다, ADR-013). 이 예외는 재시도 대상이 아니다.
- [`good/PaymentGatewayStatusResponse.java`](good/PaymentGatewayStatusResponse.java) - 결제사 전용 응답 record. `@JsonProperty` 는 여기에만 달고, 필수 칸 판단(`hasRequiredFields`)과 도메인 값 `PaymentStatus` 로 바꾸는 일도 어댑터 쪽 타입이 한다. `PaymentGatewayResponse` 는 `hasRequiredFields()` 하나를 든 어댑터 안 interface 다.

설정 파일은 이렇게 적는다(로컬과 테스트 환경 파일은 `base-url` 을 덮는다, ADR-003).

```yaml
payment-gateway:
  base-url: https://pg.example.com
  timeout:
    connect: 2s
    read: 5s
```

## 어긋난 모양

- [`bad/PaymentApprovalService.java`](bad/PaymentApprovalService.java) - 서비스가 `RestClient.create(...)` 로 타임아웃 없는 클라이언트를 만들어 승인을 부르고, 승인에 재시도를 걸고, 도메인 `Payment` 를 그대로 보내고 받는다.

## 무엇이 다른가

| 어긋난 모양 | 바른 모양 | 해당 체크리스트 줄 |
|---|---|---|
| 서비스 안의 `RestClient.create(...)` | 설정의 클라이언트 빈을 어댑터가 주입받는다 | 클라이언트를 바깥 어댑터와 설정 밖에서 쓰지 않는다 |
| 타임아웃 없음(라이브러리 기본값) | `HttpClientSettings.withTimeouts(connect, read)` 를 설정 객체 값으로 | 클라이언트마다 연결, 읽기 타임아웃이 설정에 적혀 있다 |
| 승인에 `@Retryable(maxRetries = 3)` | 상태 조회에만, 그것도 `PaymentGatewayUnavailableException` 일 때만 | 멱등이 아닌 쓰기 호출에 재시도가 없고, 429 를 제외한 4xx 는 재시도하지 않는다 |
| `.body(payment)`, `.body(Payment.class)` | `PaymentGatewayApproveRequest.of(payment)`, `PaymentGatewayStatusResponse` | 바깥 요청, 응답이 전용 타입이고 도메인 타입을 바로 직렬화하지 않는다 |
| `RestClientException` 이 서비스 밖으로 | 어댑터의 `translate(...)` 가 사유 예외로 감싼다(`cause` 포함) | 라이브러리 예외가 어댑터 밖으로 나가지 않는다 |

- 저장소에 `RestTemplate`, `WebClient`, OkHttp 를 하나 더 들이는 것도 같은 위반이다(클라이언트가 한 종류다). 이 예시는 코드로 따로 두지 않았다.
- 응답 크기 상한과 클라이언트 테스트(4xx, 5xx, 429, 연결 실패, 읽기 타임아웃이 각각 어느 사유 예외로 가는지)는 이 예시 밖이다. 테스트는 ADR-012 대로 클라이언트 통합 테스트(가짜 서버)로 둔다.

## 기계 검사

리뷰가 체크리스트로 본다. [`checks/`](../../checks/README.md) 견본에는 이 ADR 을 막는 규칙이 없다.

## 공개 참고

- spring-boot 문서 [`restclient/ssl/settings/MyService`](https://github.com/spring-projects/spring-boot/blob/main/documentation/spring-boot-docs/src/main/java/org/springframework/boot/docs/io/restclient/restclient/ssl/settings/MyService.java) - 클라이언트마다 타임아웃을 `HttpClientSettings` 로 눈에 보이게 준다. 이 예시의 `PaymentGatewayClientConfig` 가 같은 API 를 쓴다.
  - 우리 ADR 과 다른 점: 서비스 생성자에서 클라이언트를 만든다, 타임아웃이 코드 상수다.
- resilience4j-spring-boot3-demo [`application.yml`](https://github.com/resilience4j/resilience4j-spring-boot3-demo/blob/master/src/main/resources/application.yml) - 실패 종류로 재시도할지를 가른다(`retryExceptions`, `ignoreExceptions`).
  - 우리 ADR 과 다른 점: 429 를 다루지 않는다.
