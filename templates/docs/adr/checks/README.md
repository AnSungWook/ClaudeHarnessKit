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
