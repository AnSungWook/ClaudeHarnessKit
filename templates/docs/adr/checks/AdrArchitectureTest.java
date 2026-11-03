package com.example.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * 공통 ADR 가운데 기계로 판정할 수 있는 규칙만 막는다. 판단이 드는 규칙(ADR-001 읽히는 이름, ADR-002 판단의 소유 등)은 리뷰가 체크리스트로 본다.
 * 쓰는 법은 같은 폴더 README.md. 패키지 이름 com.example 은 프로젝트 기본 패키지로 바꾼다. Spring 이 클래스패스에 있다고 전제한다.
 */
@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeTests.class)
class AdrArchitectureTest {

    private static final Set<String> TIME_TYPES = Set.of(
        Instant.class.getName(), LocalDate.class.getName(), LocalDateTime.class.getName(), LocalTime.class.getName(),
        OffsetDateTime.class.getName(), OffsetTime.class.getName(), ZonedDateTime.class.getName(),
        Year.class.getName(), YearMonth.class.getName(), MonthDay.class.getName());

    /**
     * ADR-005, ADR-017: 세 층(adapter, application, domain) 사이의 역방향 의존을 막는다.
     * 같은 층 안의 의존, 기능 사이 호출(ADR-017 의 port/in), 도메인의 바깥 라이브러리 참조는 이 규칙이 안 본다 - 리뷰가 본다.
     */
    @ArchTest
    static final ArchRule dependenciesPointInward = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .withOptionalLayers(true)
        .layer("domain").definedBy("..domain..")
        .layer("application").definedBy("..application..")
        .layer("adapter").definedBy("..adapter..")
        .whereLayer("adapter").mayNotBeAccessedByAnyLayer()
        .whereLayer("application").mayOnlyBeAccessedByLayers("adapter")
        .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "adapter");

    /** ADR-008: 의존은 생성자로 받는다. */
    @ArchTest
    static final ArchRule noFieldInjection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    /** ADR-003: 설정값은 설정 record 로 바인딩한다. 필드의 @Value 금지. */
    @ArchTest
    static final ArchRule noValueOnFields = noFields().should().beAnnotatedWith(Value.class);

    /** ADR-003: 생성자·메서드 매개변수의 @Value 도 금지. */
    @ArchTest
    static final ArchRule noValueOnParameters = noCodeUnits().should(haveParameterAnnotatedWithValue());

    /** ADR-003: 현재 시각은 주입한 Clock 에서만. java.time 의 now(...) 는 now(clock) 만 되고, System.currentTimeMillis() 는 안 된다. */
    @ArchTest
    static final ArchRule noSystemClockReads = noClasses().should().callMethodWhere(readSystemClock());

    /** ADR-009: Optional 은 반환값에만. 필드 금지. */
    @ArchTest
    static final ArchRule noOptionalFields = noFields().should().haveRawType(Optional.class);

    /** ADR-011: System.out, System.err, printStackTrace 대신 로거. */
    @ArchTest
    static final ArchRule noStandardStreams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /** ADR-016: 서비스 빈 이름은 Service 로 끝난다. 서비스가 아직 없는 모듈은 통과. */
    @ArchTest
    static final ArchRule servicesEndWithService = classes().that().areAnnotatedWith(Service.class)
        .should().haveSimpleNameEndingWith("Service")
        .allowEmptyShould(true);

    /** ADR-016: 뭉뚱그린 이름 금지. Processor, Handler 는 프레임워크 계약 예외가 있어 리뷰가 본다. */
    @ArchTest
    static final ArchRule noVagueTypeNames = noClasses().should().haveSimpleNameEndingWith("Manager")
        .orShould().haveSimpleNameEndingWith("Helper")
        .orShould().haveSimpleNameEndingWith("Util")
        .orShould().haveSimpleNameEndingWith("Utils")
        .orShould().haveSimpleNameEndingWith("Common")
        .orShould().haveSimpleNameEndingWith("Base")
        .orShould().haveSimpleNameEndingWith("Data")
        .orShould().haveSimpleNameEndingWith("Info")
        .orShould().haveSimpleNameEndingWith("Dto")
        .orShould().haveSimpleNameEndingWith("Vo");

    private static DescribedPredicate<JavaMethodCall> readSystemClock() {
        return new DescribedPredicate<>("read the system clock without an injected Clock") {
            @Override
            public boolean test(JavaMethodCall call) {
                String owner = call.getTargetOwner().getName();
                String name = call.getTarget().getName();
                if (owner.equals(System.class.getName())) {
                    return name.equals("currentTimeMillis");
                }
                if (!TIME_TYPES.contains(owner) || !name.equals("now")) {
                    return false;
                }
                List<JavaClass> parameters = call.getTarget().getRawParameterTypes();
                return !(parameters.size() == 1 && parameters.get(0).isEquivalentTo(Clock.class));
            }
        };
    }

    private static ArchCondition<JavaCodeUnit> haveParameterAnnotatedWithValue() {
        return new ArchCondition<>("have a parameter annotated with @Value") {
            @Override
            public void check(JavaCodeUnit codeUnit, ConditionEvents events) {
                boolean annotated = codeUnit.getParameters().stream()
                    .anyMatch(parameter -> parameter.isAnnotatedWith(Value.class));
                events.add(new SimpleConditionEvent(codeUnit, annotated,
                    codeUnit.getFullName() + (annotated ? " has" : " has no") + " @Value parameter"));
            }
        };
    }
}
