# 기계 검사 견본

공통 ADR 가운데 **기계로 판정할 수 있는 규칙**만 빌드에서 막는 견본이다. 판단이 드는 규칙(ADR-001 읽히는 이름, ADR-002 판단의 소유, ADR-006 범용 예외의 쓰임 등)은 막지 못하니 리뷰가 각 ADR 의 체크리스트로 본다.
견본이라 프로젝트에 맞게 복사해 쓴다. 처음 넣을 때 기존 위반이 많으면 그 규칙만 잠시 빼고, 새 위반이 늘지 않게 한 뒤 하나씩 켠다.

| 파일 | 막는 것 | ADR |
|---|---|---|
| `AdrArchitectureTest.java` | 세 층(adapter, application, domain) 사이의 역방향 의존. 같은 층 안, 기능 사이 호출, 도메인의 바깥 라이브러리 참조는 리뷰가 본다 | 005, 017 |
| | 필드 주입 | 008 |
| | `@Value`(필드, 매개변수) | 003 |
| | `java.time` 타입의 `now(...)` 가운데 `now(clock)` 이 아닌 것(`now()`, `now(ZoneId)`), `System.currentTimeMillis()` | 003 |
| | `Optional` 필드 | 009 |
| | `System.out`, `System.err`, `printStackTrace` | 011 |
| | `@Service` 인데 이름이 `Service` 로 안 끝남 | 016 |
| | `Manager`, `Helper`, `Util`, `Utils`, `Common`, `Base`, `Data`, `Info`, `Dto`, `Vo` 로 끝나는 타입 | 016 |
| | **(관례)** `@EventListener`(메타 어노테이션 포함, `@TransactionalEventListener` 와 합성 어노테이션) 메서드와 `ApplicationListener` 구현(상속 포함)이 `adapter.in.event` 밖에 있음. 생명주기 이벤트만 받는 리스너는 `config` 에도 둘 수 있음 | 001, 017 |
| | **(관례)** 입구 어노테이션(`@Scheduled`, `@Schedules`, `@KafkaListener(s)`, `@RabbitListener(s)`, `@JmsListener(s)` - 완전 수식 이름 목록)이 달린 메서드·클래스, `ApplicationRunner`·`CommandLineRunner` 구현이 `adapter.in` 밖에 있음 | 017 |
| | **(관례)** `@Aspect` 가 `..observability..` 밖에 있고 허용 목록(`ALLOWED_ASPECTS`)에도 없음 | 001 |
| | 검사 대상 클래스가 아예 안 들어옴(위 관례 검사가 빈 집합으로 통과하는 것과 가르려고) | - |
| `fixtures/` | 위 관례 검사가 바른 모양은 통과시키고 어긋난 모양은 잡는지 확인하는 테스트(`AdrArchitectureFixtureTest`)와 fixture 소스 | 001, 017 |
| `checkstyle-adr.xml` | 와일드카드 import | 007 |
| `commitlint.config.js` | 커밋 제목 `type(scope): subject`, type 목록, scope 는 영문 소문자와 하이픈, 끝 마침표. 머지 커밋 말고는 무시하지 않음 | 020 |

## ArchUnit (Java, Gradle, Spring)

견본은 Spring 어노테이션(`@Service`, `@Value`, `@Autowired`)을 보므로 Spring 이 클래스패스에 있는 프로젝트를 전제한다.

1. 의존과 JUnit Platform 실행을 더한다(Spring Boot 프로젝트는 `spring-boot-starter-test` 가 JUnit 을 이미 든다).
   ```groovy
   dependencies {
       testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
       testImplementation 'org.junit.jupiter:junit-jupiter'          // Spring Boot 가 아니면 버전을 적는다
       testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }
   tasks.withType(Test).configureEach { useJUnitPlatform() }
   ```
2. `AdrArchitectureTest.java` 를 `src/test/java/<기본 패키지>/architecture/` 에 복사하고, `package` 줄과 `@AnalyzeClasses(packages = ...)` 를 프로젝트 기본 패키지로 바꾼다.
3. `./gradlew test` 에서 함께 돈다. 위반이 있으면 테스트가 실패하고 어긴 클래스와 줄을 알려 준다.

- 층 규칙은 기본 배치(ADR-017 의 `domain`, `application`, `adapter`) 기준이다. 다른 배치를 고른 저장소는 `definedBy(...)` 의 패키지 패턴을 그 배치의 같은 역할 폴더로 바꾼다.
- 테스트 코드는 검사하지 않는다(`DoNotIncludeTests`). 테스트에서 `Instant.now()` 를 쓰는 것은 ADR-003 이 허용한다.
- 다른 언어: Kotlin 은 같은 ArchUnit, TypeScript 는 dependency-cruiser, Python 은 import-linter 로 같은 층 규칙을 걸 수 있다.

### 흐름 관례 검사 (ADR-001 「서비스에 안 보이는 흐름」, ADR-017 입구)

ADR 에는 원칙(무엇을 막나)만 있고, 자리·모양·허용 목록은 이 견본의 **관례**다. 검사를 통과했다고 흐름이 읽힌다는 보장은 없다 - 읽히는지는 리뷰가 ADR-001 체크리스트로 본다.

- 고칠 자리(파일 위쪽 상수): 이벤트 입구 `EVENT_ADAPTERS`, 입구 `INBOUND_ADAPTERS`, 생명주기 리스너 자리 `CONFIG`, 관측 애스펙트 자리 `OBSERVABILITY_ASPECTS`, 허용 애스펙트 `ALLOWED_ASPECTS`, 입구 어노테이션 `ENTRY_ANNOTATIONS`. 다른 배치를 고른 저장소는 그 배치의 같은 역할 폴더로 바꾼다.
- 입구 어노테이션은 완전 수식 이름 문자열로 비교한다. 메시지 라이브러리를 필수 의존으로 만들지 않으려는 것이다. 쓰는 라이브러리의 수신 어노테이션과 그 반복 선언 컨테이너를 목록에 더한다.
- 규칙은 자리와 목록을 받는 팩토리(`eventListenerMethodsResideIn`, `entryClassesResideIn` 등)로 만들어 두었다. `@ArchTest` 필드는 위 상수로 그 팩토리를 부른다. 배치나 목록을 바꿀 때는 상수만 고친다.
- 잡는 것: 메서드·클래스에 직접 단 것, 메타 어노테이션(합성 어노테이션), 반복 선언 컨테이너, 상속으로 구현한 `ApplicationListener`.
- 생명주기 리스너 판정(`CONFIG` 에 둘 수 있는가):
  - 받는 이벤트 타입은 `@EventListener`·`@TransactionalEventListener` 의 `classes`/`value`(직접 단 것과, 속성 없는 합성 어노테이션 안에 고정한 것)가 정하고, 적혀 있지 않으면 매개변수 하나가 정한다. 그 타입이 모두 생명주기 이벤트일 때만 `CONFIG` 에 둘 수 있다.
  - **속성을 하나라도 가진 합성 리스너 어노테이션은 생명주기 예외를 주지 않는다.** 그 속성이 `@AliasFor` 로 고정한 이벤트 타입을 덮어쓸 수 있는데 검사가 별칭을 풀지 않기 때문이다(보수적). 그런 리스너는 `adapter.in.event` 에 둔다.
  - `ApplicationListener` 는 구현한 타입 인자(상속 사슬의 첫 `ApplicationListener<…>`)로 본다. 타입 인자가 없는 날것(raw) 구현은 업무 리스너로 본다.
- **놓치는 것(리뷰가 본다):**
  - 코드로 등록한 리스너·스케줄러·Advisor(`addApplicationListener`, `TaskScheduler.schedule`, `SchedulingConfigurer`, 프로그램 등록 `Advisor`), XML 설정
  - 함수형 HTTP 라우팅(`RouterFunction`), 서블릿 필터·인터셉터의 자리
  - 허용된 애스펙트 안에 새로 넣은 advice, 애스펙트가 관측만 하는지(결과를 안 바꾸는지)
  - 리스너가 유스케이스에 넘기기만 하는지, `@Order` 에 업무 순서를 숨겼는지, 실패 처리가 정해졌는지
  - 같은 층 안 의존(어댑터끼리 호출) - `dependenciesPointInward` 도 못 잡는다
- 관례 검사는 대상이 없으면 통과한다(`allowEmptyShould(true)`). 그래서 `applicationClassesAreImported` 가 검사 대상 클래스가 아예 안 들어오는 경우(기본 패키지를 잘못 적음)를 따로 잡는다.

### fixture 테스트

`fixtures/src/test/java/` 를 프로젝트의 `src/test/java/` 에 그대로 복사하면 `AdrArchitectureFixtureTest` 가 관례 검사를 바른 모양(`fixture.good`)과 어긋난 모양(`fixture.bad.*`)에 돌린다. 상수(자리, 목록)를 바꿨을 때 검사가 여전히 맞게 도는지 확인하는 용도다.

- 바른 모양은 빈 집합을 실패로 보고(`allowEmptyShould(false)`) 돌린다 - 대상이 하나도 안 잡혀 그냥 통과하는 것을 막는다.
- 어긋난 모양은 실패 메시지에 기대한 클래스·메서드가 있는지까지 본다 - 다른 이유로 실패해 통과하는 것을 막는다(예: 상속 fixture 는 부모를 바른 자리에 두고 자식만 걸리게 했다).
- 다루는 모양: 서비스 안 리스너, 합성 어노테이션 리스너, 상속한 `ApplicationListener`, 생명주기 리스너 다섯 모양(매개변수, 어노테이션 값, 어노테이션 값 + 넓은 매개변수, 속성 없는 합성 어노테이션에 고정한 값, 타입 인자)과 예외를 주면 안 되는 두 모양(별칭 아닌 합성 속성, 별칭으로 고정값 덮어쓰기), 스케줄 트리거(단일, 반복 선언), 클래스 수준 수신 선언(직접, 합성, 반복 선언), 명령행 입구, 애스펙트(관측 자리, 허용 목록), 대체 배치(`..listener..` 를 고른 저장소).
- 클래스 수준 수신 선언은 메시지 라이브러리를 필수 의존으로 만들지 않으려고 fixture 용 어노테이션(`fixture.FixtureMessageListener`, 반복 선언 컨테이너 `FixtureMessageListeners`)으로 확인한다. 실제 라이브러리 어노테이션은 같은 판정 코드에 이름만 다르게 들어간다.
- fixture 는 테스트 소스라 `AdrArchitectureTest` 의 기본 검사(`DoNotIncludeTests`)에는 안 들어간다.
- 필요한 의존: `spring-context`, `spring-tx`, `spring-boot`, `aspectjweaver`(또는 `aspectjrt`). Spring Boot 프로젝트면 이미 있다.
- 확인한 환경: Spring Boot 4.1.2, ArchUnit 1.3.0, Java 21 에서 fixture 테스트 14개와 기본 검사 16개 통과(2026-11-04).

## Checkstyle (와일드카드 import)

```groovy
plugins { id 'checkstyle' }
checkstyle {
    configFile = file('config/checkstyle/checkstyle-adr.xml')
}
```

`checkstyle-adr.xml` 을 `config/checkstyle/` 에 복사한다. 이미 Checkstyle 을 쓰면 `AvoidStarImport` 한 줄만 기존 설정에 더한다.

## commitlint (커밋 제목)

```bash
npm install --save-dev @commitlint/cli @commitlint/config-conventional
cp docs/adr/checks/commitlint.config.js .
npx commitlint --from origin/main --to HEAD     # 브랜치의 커밋을 한 번에 검사
```

- PR 검사(CI)에 마지막 명령을 걸면 사람마다 훅을 깔지 않아도 된다. 로컬에서 바로 막고 싶으면 husky 의 `commit-msg` 훅에 `npx --no -- commitlint --edit "$1"` 을 건다.
- scope 는 영문 소문자와 하이픈만(ADR-020, `scope-lowercase-hyphen` 규칙), subject 는 한국어여도 된다.
- 머지 커밋 제목 말고는 무시하지 않는다(`defaultIgnores: false`). `Revert "..."`, `fixup! ...` 도 검사에 걸리니 브랜치를 올리기 전에 정리한다.
