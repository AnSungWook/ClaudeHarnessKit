# ADR-022: 정체를 정하는 어노테이션은 선언 바로 위에 두고, 멤버는 public 하나 밑에 그것이 부르는 private 을 둔다

## Status

Accepted

## Date

2026-11-02

## Intent

선언 바로 위 한 줄만 보고 그 클래스나 메서드가 무엇인지 알게 하고, 위에서 아래로 한 번 읽으면 public 하나와 그 단계가 붙어서 읽히게 한다.

## Context

- 기존 상태: 어노테이션 순서가 사람마다 달라 `@Service` 가 줄 중간에 묻히거나 맨 위에 온다. private 메서드가 파일 끝에 몰려 있어 public 하나가 하는 일을 보려면 오르내려야 한다.
- 문제점: 같은 종류의 클래스가 diff 에서 다른 모양으로 보이고, 순서만 바꾸는 리뷰 지적이 반복된다. public 과 그 단계가 떨어져 흐름이 안 읽힌다.
- 제약 조건: 포매터(IntelliJ arrangement 등)는 메서드를 정적, 인스턴스나 이름 순으로만 묶을 수 있어 "부르는 순서" 는 사람이 지켜야 한다.

## Decision

**어노테이션은 정체가 선언에 가장 가깝게, 멤버는 public 과 그것이 부르는 private 이 붙어 있게.**

### 어노테이션 순서

- 클래스 위 어노테이션은 위에서 아래로 이 순서다. 그 클래스가 무엇인지 정하는 어노테이션이 선언 바로 위에 온다.
  1. 로그: `@Slf4j`
  2. 접근자와 표시: `@Getter`, `@ToString`, `@EqualsAndHashCode`
  3. 생성자: `@RequiredArgsConstructor`, `@NoArgsConstructor`
  4. 실행 범위: `@StepScope`, `@JobScope`(`@Transactional` 은 클래스가 아니라 서비스 메서드에 둔다, ADR-014)
  5. 그 정체에 딸린 설정: `@RequestMapping`, `@Table`, `@ConfigurationProperties`
  6. 정체: `@Service`, `@Repository`, `@Component`, `@RestController`, `@Configuration`, `@Entity`

```java
@Slf4j
@RequiredArgsConstructor
@Service
public class OrderQueryService {

    @Transactional(readOnly = true)
    public OrderResult getOrder(long orderId) {
```

- 멤버도 같은 원칙이다. 비동기 이벤트 수신은 `@Async` 다음 줄에 `@EventListener`. 스코프가 있는 빈은 `@StepScope` 다음 줄에 `@Bean`. 요청 record 칸은 검증 어노테이션 다음 줄에 `@JsonProperty`. `@Override` 는 맨 아래, 선언 바로 위다.
- 테스트는 프로젝트가 이미 한 모양으로 쓰고 있으면 그 모양을 따른다(예: `@Test` 다음 줄에 `@DisplayName`). 새 프로젝트면 정체 원칙대로 `@DisplayName` 다음 줄에 `@Test` 다. 한 저장소 안에서 섞지 않는 것이 먼저다.
- 목록에 없는 어노테이션은 정체와 얼마나 가까운가로 자리를 정한다. 정체 어노테이션보다 아래에 두지 않는다.

### 멤버 순서

- 일반 클래스는 위에서 아래로 상수, 필드, 생성자, 정적 팩토리, public 메서드, 중첩 타입 순이다.
- 각 public 바로 아래에 그 메서드가 부르는 private 을 부르는 순서대로 둔다. 여러 public 이 같이 쓰는 private 은 처음 부르는 public 아래에 둔다. 파일 끝에 private 을 몰아 두지 않는다.
- 엔티티는 종류별 자리를 고정한다. 필드, 정적 팩토리, 연관 할당, 상태 변경, 판단(`is...`), 계산된 값, 도메인 값 변환(`to...`) 순이다.
  - 이 순서는 public 멤버끼리의 순서다. 각 public 밑에 그것이 부르는 private(private 생성자, 검증 메서드 포함)을 두는 위 규칙이 먼저다.
  - `to...` 는 다른 도메인 값으로 바꾸는 것만이다. 응답으로 바꾸는 `toResponse()` 는 엔티티에 두지 않는다(ADR-002).
- 포매터의 정렬(arrangement)은 상수, 필드, 생성자, 중첩 타입 자리만 맞추고 메서드는 정렬 규칙에서 뺀다. 메서드 자리는 사람이 지킨다(ADR-007).
- 새로 쓰거나 고친 클래스부터 따른다. 순서만 바꾸는 커밋을 기능 커밋과 섞지 않는다.

### 체크리스트

- [ ] 정체 어노테이션(`@Service`, `@Entity` 등) 다음 줄에 다른 어노테이션이 없다
- [ ] `@Override` 가 선언 바로 위다
- [ ] 한 저장소 안에서 테스트 어노테이션 순서가 한 모양이다
- [ ] private 메서드가 그것을 처음 부르는 public 아래에 있다(파일 끝에 몰려 있지 않다)
- [ ] 엔티티도 private 이 그것을 부르는 public 아래에 있고, 엔티티 자리 순서는 public 멤버끼리 맞는다
- [ ] 엔티티의 `to...` 가 도메인 값 변환뿐이다(`toResponse()` 가 없다)
- [ ] `@Transactional` 이 클래스가 아니라 서비스 메서드에 있다

### 안티패턴

| 안티패턴 | 올바른 패턴 |
|---------|-----------|
| `@Service` 다음 줄에 `@RequiredArgsConstructor`, `@Slf4j` | `@Slf4j`, `@RequiredArgsConstructor`, `@Service` 순 |
| `@EventListener` 다음 줄에 `@Async` | `@Async` 다음 줄에 `@EventListener` |
| public 메서드 다섯 뒤에 private 열 개를 몰아 둠 | public 하나 밑에 그것이 부르는 private 을 부르는 순서대로 |
| 알파벳 순으로 어노테이션 정렬 | 정체에서 먼 것부터 가까운 것 순 |

## 다른 언어에서

- 원칙(정체를 말하는 표시가 선언에 가장 가깝게, 공개 진입점과 그 단계가 붙어 있게)은 같다.
- Kotlin: Spring 어노테이션은 같은 순서. 멤버는 Kotlin 공식 컨벤션(프로퍼티, init, 보조 생성자, 메서드, companion object)을 따르되 public 아래 그 private 원칙은 같다.
- TypeScript: 데코레이터(NestJS 의 `@Injectable`, `@Controller`)는 정체가 클래스에 가장 가깝게. 모듈 안에서는 export 하는 함수 아래 그것이 쓰는 내부 함수를 둔다.
- Python: 데코레이터는 아래에서 위로 적용되니, 정체를 정하는 것(`@app.get`, `@dataclass`)이 함수에 가장 가깝게 오는지 프레임워크 문서대로 본다. 모듈 안 함수 순서는 같은 원칙.

## Consequences

### 긍정적
- 같은 종류의 클래스가 같은 모양으로 보여 diff 와 리뷰가 빨라진다.
- public 하나와 그 단계가 한 화면에 붙어 흐름이 읽힌다.

### 부정적
- 메서드 순서는 도구로 강제하기 어려워 리뷰가 봐야 한다.
- 기존 파일과 한동안 두 모양이 같이 산다.

## References

- ADR-002(엔티티의 응답 변환 금지), ADR-007(코드 모양, 바꾼 줄만), ADR-008(생성자 주입), ADR-010(주석), ADR-014(트랜잭션은 서비스 메서드에)
