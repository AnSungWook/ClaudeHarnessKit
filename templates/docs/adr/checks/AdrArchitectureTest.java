package com.example.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
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

    // ---- ADR-001, ADR-017: 서비스에 안 보이는 흐름 - 리스너·입구·애스펙트의 자리 (킷의 관례) ----
    // 아래 상수가 이 저장소의 배치다. 다른 배치를 고른 저장소는 그 배치의 같은 역할 폴더로 바꾼다.
    // 규칙은 자리와 목록을 받는 팩토리로 만들어, 검사를 고칠 때 다른 배치나 시험용 어노테이션으로도 같은 판정을 확인할 수 있게 한다.

    /** 이벤트 리스너를 두는 자리. */
    static final String EVENT_ADAPTERS = "..adapter.in.event..";

    /** 입구를 두는 자리(인바운드 어댑터 역할 폴더). */
    static final String INBOUND_ADAPTERS = "..adapter.in..";

    /** 생명주기 리스너를 둘 수 있는 자리. 업무 리스너는 EVENT_ADAPTERS 에만. */
    static final String CONFIG = "..config..";

    /**
     * 입구 어노테이션의 완전 수식 이름. 라이브러리 의존 없이 이름으로 비교한다.
     * 반복 선언 컨테이너(Schedules, KafkaListeners)도 넣는다. 쓰는 메시지 라이브러리의 수신 어노테이션은 프로젝트가 더한다.
     */
    static final Set<String> ENTRY_ANNOTATIONS = Set.of(
        "org.springframework.scheduling.annotation.Scheduled",
        "org.springframework.scheduling.annotation.Schedules",
        "org.springframework.kafka.annotation.KafkaListener",
        "org.springframework.kafka.annotation.KafkaListeners",
        "org.springframework.amqp.rabbit.annotation.RabbitListener",
        "org.springframework.amqp.rabbit.annotation.RabbitListeners",
        "org.springframework.jms.annotation.JmsListener",
        "org.springframework.jms.annotation.JmsListeners");

    /** 명령행 입구. Spring Boot 타입이라 이름으로 비교한다. */
    static final Set<String> RUNNER_TYPES = Set.of(
        "org.springframework.boot.ApplicationRunner",
        "org.springframework.boot.CommandLineRunner");

    /** 생명주기 이벤트의 부모 타입. 이것만 받는 리스너는 CONFIG 에도 둘 수 있다. */
    static final Set<String> LIFECYCLE_EVENT_TYPES = Set.of(
        "org.springframework.context.event.ApplicationContextEvent",
        "org.springframework.boot.context.event.SpringApplicationEvent",
        "org.springframework.boot.availability.AvailabilityChangeEvent");

    /** 표시 없이 패턴으로 거는 관측 애스펙트를 두는 자리. 그 밖의 애스펙트는 ALLOWED_ASPECTS 에 완전 수식 이름으로 올린다. */
    static final String OBSERVABILITY_ASPECTS = "..observability..";

    /** 표시 어노테이션만 처리하는 애스펙트처럼, 리뷰를 거쳐 허용한 애스펙트의 완전 수식 이름. */
    static final Set<String> ALLOWED_ASPECTS = Set.of();

    private static final String ASPECT = "org.aspectj.lang.annotation.Aspect";
    private static final String EVENT_LISTENER = "org.springframework.context.event.EventListener";
    private static final String APPLICATION_LISTENER = "org.springframework.context.ApplicationListener";

    /** classes/value 가 받는 이벤트 타입을 뜻하는 것이 확인된 어노테이션. 이 밖의 합성 어노테이션 속성은 이벤트 타입으로 읽지 않는다. */
    private static final Set<String> EVENT_TYPE_DECLARING_ANNOTATIONS = Set.of(
        EVENT_LISTENER,
        "org.springframework.transaction.event.TransactionalEventListener");

    /**
     * ADR-001, ADR-017: @EventListener(메타 어노테이션으로 단 @TransactionalEventListener, 합성 어노테이션 포함) 메서드는 이벤트 입구 폴더에만.
     * 생명주기 이벤트만 받는 메서드는 CONFIG 에도 둘 수 있다. 리스너가 아직 없는 모듈은 통과.
     */
    @ArchTest
    static final ArchRule eventListenerMethodsResideInEventAdapters = eventListenerMethodsResideIn(EVENT_ADAPTERS, CONFIG);

    /** ADR-001, ADR-017: ApplicationListener 구현(직접 구현, 상속한 구현 모두)도 같은 자리에. 생명주기 이벤트 리스너는 CONFIG 에도. */
    @ArchTest
    static final ArchRule applicationListenersResideInEventAdapters = applicationListenersResideIn(EVENT_ADAPTERS, CONFIG);

    /** ADR-017: 스케줄·메시지 수신 입구 메서드(직접, 메타 어노테이션, 반복 선언 컨테이너)는 인바운드 어댑터 폴더에만. */
    @ArchTest
    static final ArchRule entryMethodsResideInInboundAdapters = entryMethodsResideIn(ENTRY_ANNOTATIONS, INBOUND_ADAPTERS);

    /** ADR-017: 클래스에 단 수신 어노테이션(예: 클래스 수준 @KafkaListener)도 같다. */
    @ArchTest
    static final ArchRule entryClassesResideInInboundAdapters = entryClassesResideIn(ENTRY_ANNOTATIONS, INBOUND_ADAPTERS);

    /** ADR-017: ApplicationRunner, CommandLineRunner 구현은 인바운드 어댑터 폴더에만. */
    @ArchTest
    static final ArchRule runnersResideInInboundAdapters = runnersResideIn(RUNNER_TYPES, INBOUND_ADAPTERS);

    /** ADR-001: @Aspect 는 관측 자리나 허용 목록에만. 허용된 애스펙트 안에 새로 넣은 advice 는 리뷰가 본다. */
    @ArchTest
    static final ArchRule aspectsAreObservabilityOrAllowListed = aspectsResideIn(OBSERVABILITY_ASPECTS, ALLOWED_ASPECTS);

    /** 위 규칙들이 빈 집합으로 통과할 때, 검사 대상 클래스가 아예 안 들어와서인지 가르려고 둔다. 기본 패키지를 바꿨으면 여기도 바꾼다. */
    @ArchTest
    static final ArchRule applicationClassesAreImported = classes()
        .should().resideInAPackage("com.example..");

    static ArchRule eventListenerMethodsResideIn(String eventAdapters, String lifecycleConfig) {
        return methods()
            .that().areMetaAnnotatedWith(EVENT_LISTENER)
            .should(resideInEventAdaptersOrConfigForLifecycle(eventAdapters, lifecycleConfig))
            .allowEmptyShould(true);
    }

    static ArchRule applicationListenersResideIn(String eventAdapters, String lifecycleConfig) {
        return classes()
            .that().areAssignableTo(APPLICATION_LISTENER).and().areNotInterfaces()
            .should(resideInEventAdaptersOrConfigForLifecycleListener(eventAdapters, lifecycleConfig))
            .allowEmptyShould(true);
    }

    static ArchRule entryMethodsResideIn(Set<String> entryAnnotations, String inboundAdapters) {
        return methods()
            .that(areMetaAnnotatedWithAny(entryAnnotations))
            .should().beDeclaredInClassesThat().resideInAPackage(inboundAdapters)
            .allowEmptyShould(true);
    }

    static ArchRule entryClassesResideIn(Set<String> entryAnnotations, String inboundAdapters) {
        return classes()
            .that(areMetaAnnotatedWithAnyClass(entryAnnotations))
            .should().resideInAPackage(inboundAdapters)
            .allowEmptyShould(true);
    }

    static ArchRule runnersResideIn(Set<String> runnerTypes, String inboundAdapters) {
        return classes()
            .that(areAssignableToAny(runnerTypes)).and().areNotInterfaces()
            .should().resideInAPackage(inboundAdapters)
            .allowEmptyShould(true);
    }

    static ArchRule aspectsResideIn(String observabilityAspects, Set<String> allowedAspects) {
        return classes()
            .that().areMetaAnnotatedWith(ASPECT)
            .should().resideInAPackage(observabilityAspects)
            .orShould(beInAllowList(allowedAspects))
            .allowEmptyShould(true);
    }

    private static ArchCondition<JavaMethod> resideInEventAdaptersOrConfigForLifecycle(String eventAdapters, String lifecycleConfig) {
        return new ArchCondition<>("be declared in " + eventAdapters + " (lifecycle listeners may live in " + lifecycleConfig + ")") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass owner = method.getOwner();
                boolean ok = resides(owner, eventAdapters)
                    || (resides(owner, lifecycleConfig) && receivesOnlyLifecycleEvents(method));
                events.add(new SimpleConditionEvent(method, ok,
                    method.getFullName() + (ok ? " is" : " is not") + " in an event adapter"));
            }
        };
    }

    private static ArchCondition<JavaClass> resideInEventAdaptersOrConfigForLifecycleListener(String eventAdapters, String lifecycleConfig) {
        return new ArchCondition<>("reside in " + eventAdapters + " (lifecycle listeners may live in " + lifecycleConfig + ")") {
            @Override
            public void check(JavaClass listener, ConditionEvents events) {
                boolean ok = resides(listener, eventAdapters)
                    || (resides(listener, lifecycleConfig) && listensOnlyToLifecycleEvents(listener));
                events.add(new SimpleConditionEvent(listener, ok,
                    listener.getName() + (ok ? " is" : " is not") + " in an event adapter"));
            }
        };
    }

    /**
     * 받는 이벤트 타입은 @EventListener 의 classes/value 가 정하고(직접 단 것, 합성 어노테이션 안에 고정한 것 모두), 적혀 있지 않으면 매개변수 하나가 정한다.
     * 그 타입이 모두 생명주기 이벤트일 때만 생명주기 리스너다.
     */
    private static boolean receivesOnlyLifecycleEvents(JavaMethod method) {
        if (method.getAnnotations().stream().anyMatch(annotation -> isComposedListenerWithAttributes(annotation.getRawType(), new java.util.HashSet<>()))) {
            // 속성이 있는 합성 리스너 어노테이션은 그 속성이 @AliasFor 로 이벤트 타입을 덮어쓸 수 있다. 별칭을 풀지 않으므로 예외를 주지 않는다(보수적).
            return false;
        }
        List<JavaClass> declared = new java.util.ArrayList<>();
        method.getAnnotations().forEach(annotation -> collectDeclaredEventTypes(annotation, declared, new java.util.HashSet<>()));
        if (!declared.isEmpty()) {
            return declared.stream().allMatch(AdrArchitectureTest::isLifecycleEvent);
        }
        List<JavaClass> parameters = method.getRawParameterTypes();
        return parameters.size() == 1 && isLifecycleEvent(parameters.get(0));
    }

    /** 이벤트 리스너를 메타 어노테이션으로 든 합성 어노테이션(사슬 어디든)이 자기 속성을 하나라도 가지면 true. */
    private static boolean isComposedListenerWithAttributes(JavaClass type, Set<String> visited) {
        if (!visited.add(type.getName()) || EVENT_TYPE_DECLARING_ANNOTATIONS.contains(type.getName())
            || !type.isMetaAnnotatedWith(EVENT_LISTENER)) {
            return false;
        }
        return !type.getMethods().isEmpty()
            || type.getAnnotations().stream().anyMatch(meta -> isComposedListenerWithAttributes(meta.getRawType(), visited));
    }

    private static void collectDeclaredEventTypes(
            com.tngtech.archunit.core.domain.JavaAnnotation<?> annotation, List<JavaClass> declared, Set<String> visited) {
        JavaClass type = annotation.getRawType();
        if (!visited.add(type.getName())) {
            return;
        }
        // Spring 이 이벤트 타입으로 읽는 속성만 본다. 합성 어노테이션의 자체 속성은 @AliasFor 로 이어졌는지 확인하지 않으므로 읽지 않는다(보수적).
        if (EVENT_TYPE_DECLARING_ANNOTATIONS.contains(type.getName())) {
            for (String key : List.of("classes", "value")) {
                annotation.get(key)
                    .filter(JavaClass[].class::isInstance)
                    .map(JavaClass[].class::cast)
                    .ifPresent(types -> declared.addAll(List.of(types)));
            }
        }
        type.getAnnotations().forEach(meta -> collectDeclaredEventTypes(meta, declared, visited));
    }

    private static boolean listensOnlyToLifecycleEvents(JavaClass listener) {
        for (JavaClass type = listener; type != null; type = type.getRawSuperclass().orElse(null)) {
            for (JavaType implemented : type.getInterfaces()) {
                if (implemented.toErasure().getName().equals(APPLICATION_LISTENER)
                    && implemented instanceof JavaParameterizedType parameterized
                    && !parameterized.getActualTypeArguments().isEmpty()) {
                    return isLifecycleEvent(parameterized.getActualTypeArguments().get(0).toErasure());
                }
            }
        }
        return false;
    }

    private static boolean isLifecycleEvent(JavaClass type) {
        return LIFECYCLE_EVENT_TYPES.stream().anyMatch(type::isAssignableTo);
    }

    private static boolean resides(JavaClass type, String packagePattern) {
        return com.tngtech.archunit.core.domain.PackageMatcher.of(packagePattern).matches(type.getPackageName());
    }

    private static DescribedPredicate<JavaMethod> areMetaAnnotatedWithAny(Set<String> annotationNames) {
        return new DescribedPredicate<>("are meta-annotated with any of " + annotationNames) {
            @Override
            public boolean test(JavaMethod method) {
                return annotationNames.stream().anyMatch(method::isMetaAnnotatedWith);
            }
        };
    }

    private static DescribedPredicate<JavaClass> areMetaAnnotatedWithAnyClass(Set<String> annotationNames) {
        return new DescribedPredicate<>("are meta-annotated with any of " + annotationNames) {
            @Override
            public boolean test(JavaClass type) {
                return !type.isAnnotation() && annotationNames.stream().anyMatch(type::isMetaAnnotatedWith);
            }
        };
    }

    private static DescribedPredicate<JavaClass> areAssignableToAny(Set<String> typeNames) {
        return new DescribedPredicate<>("are assignable to any of " + typeNames) {
            @Override
            public boolean test(JavaClass type) {
                return typeNames.stream().anyMatch(type::isAssignableTo);
            }
        };
    }

    private static ArchCondition<JavaClass> beInAllowList(Set<String> allowed) {
        return new ArchCondition<>("be in the allow list " + allowed) {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                boolean ok = allowed.contains(type.getName());
                events.add(new SimpleConditionEvent(type, ok, type.getName() + (ok ? " is" : " is not") + " allow-listed"));
            }
        };
    }

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
