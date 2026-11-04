package com.example.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 흐름 규칙(ADR-001, ADR-017 관례)이 바른 모양은 통과시키고 어긋난 모양은 잡는지 fixture 로 확인한다.
 * 어긋난 모양은 실패 메시지에 기대한 클래스가 있는지까지 본다 - 다른 이유로 실패해 통과하는 것을 막는다.
 * fixture 는 테스트 소스라 AdrArchitectureTest 의 기본 검사(DoNotIncludeTests)에는 안 들어간다.
 */
class AdrArchitectureFixtureTest {

    private static final String FIXTURE = "com.example.architecture.fixture.";

    /** 메시지 라이브러리 없이 클래스 수준 수신 선언을 보려고 fixture 용 어노테이션을 입구 목록에 넣는다. */
    private static final Set<String> FIXTURE_ENTRY_ANNOTATIONS = Set.of(
        FIXTURE + "FixtureMessageListener",
        FIXTURE + "FixtureMessageListeners");

    private static final ArchRule FIXTURE_ENTRY_CLASSES =
        AdrArchitectureTest.entryClassesResideIn(FIXTURE_ENTRY_ANNOTATIONS, AdrArchitectureTest.INBOUND_ADAPTERS);

    @Test
    @DisplayName("바른 모양은 흐름 규칙을 모두 통과한다 - 검사 대상이 비어 그냥 통과하지 않게 빈 집합을 실패로 본다")
    void goodFixturesPassFlowRules() {
        JavaClasses good = importPackage("good");
        AdrArchitectureTest.eventListenerMethodsResideInEventAdapters.allowEmptyShould(false).check(good);
        AdrArchitectureTest.applicationListenersResideInEventAdapters.allowEmptyShould(false).check(good);
        AdrArchitectureTest.entryMethodsResideInInboundAdapters.allowEmptyShould(false).check(good);
        AdrArchitectureTest.runnersResideInInboundAdapters.allowEmptyShould(false).check(good);
        AdrArchitectureTest.aspectsAreObservabilityOrAllowListed.allowEmptyShould(false).check(good);
        FIXTURE_ENTRY_CLASSES.allowEmptyShould(false).check(good);
    }

    @Test
    @DisplayName("서비스 안의 리스너를 잡는다")
    void catchesListenerInService() {
        assertViolates(AdrArchitectureTest.eventListenerMethodsResideInEventAdapters, "bad.listener", "OrderService.sendReceiptOnOrderPlaced");
    }

    @Test
    @DisplayName("합성 어노테이션으로 단 리스너도 잡는다")
    void catchesMetaAnnotatedListener() {
        assertViolates(AdrArchitectureTest.eventListenerMethodsResideInEventAdapters, "bad.metalistener", "OrderService.recordPointOnOrderPlaced");
    }

    @Test
    @DisplayName("상속으로 구현한 ApplicationListener 를 잡는다 - 부모는 바른 자리라 자식만 걸린다")
    void catchesInheritedApplicationListener() {
        assertViolates(AdrArchitectureTest.applicationListenersResideInEventAdapters, "bad.applistener", "application.service.OrderListener");
    }

    @Test
    @DisplayName("config 밖의 생명주기 리스너를 잡는다")
    void catchesLifecycleListenerOutsideConfig() {
        assertViolates(AdrArchitectureTest.eventListenerMethodsResideInEventAdapters, "bad.lifecycle", "StartupService.warmUpOnReady");
    }

    @Test
    @DisplayName("합성 어노테이션의 별칭 아닌 속성으로 생명주기 예외를 얻은 config 의 업무 리스너를 잡는다")
    void catchesBusinessListenerHidingBehindNonAliasedAttribute() {
        assertViolates(AdrArchitectureTest.eventListenerMethodsResideInEventAdapters, "bad.customalias", "OrderPlacedOnConfig.recordOrderOnOrderPlaced");
    }

    @Test
    @DisplayName("별칭으로 고정값을 덮어쓴 합성 어노테이션의 config 업무 리스너를 잡는다")
    void catchesBusinessListenerOverridingFixedLifecycleType() {
        assertViolates(AdrArchitectureTest.eventListenerMethodsResideInEventAdapters, "bad.aliasoverride", "OrderPlacedOverrideOnConfig.recordOrderOnOrderPlaced");
    }

    @Test
    @DisplayName("서비스 안의 스케줄 트리거를 잡는다")
    void catchesScheduledInService() {
        assertViolates(AdrArchitectureTest.entryMethodsResideInInboundAdapters, "bad.schedule", "SettlementService.startDailySettlement");
    }

    @Test
    @DisplayName("반복 선언 컨테이너로 단 스케줄 트리거를 잡는다")
    void catchesRepeatedScheduledInService() {
        assertViolates(AdrArchitectureTest.entryMethodsResideInInboundAdapters, "bad.schedules", "SettlementService.startDailySettlement");
    }

    @Test
    @DisplayName("서비스에 단 클래스 수준 수신 선언을 잡는다(직접, 합성, 반복 선언)")
    void catchesClassLevelEntryInService() {
        assertViolates(FIXTURE_ENTRY_CLASSES, "bad.classentry", "OrderMessageService");
        assertViolates(FIXTURE_ENTRY_CLASSES, "bad.classmeta", "OrderTopicService");
        assertViolates(FIXTURE_ENTRY_CLASSES, "bad.classcontainer", "OrderTopicsService");
    }

    @Test
    @DisplayName("config 의 명령행 입구를 잡는다")
    void catchesRunnerInConfig() {
        assertViolates(AdrArchitectureTest.runnersResideInInboundAdapters, "bad.runner", "ImportOrdersRunner");
    }

    @Test
    @DisplayName("관측 자리 밖이고 허용 목록에도 없는 애스펙트를 잡는다")
    void catchesUnlistedAspect() {
        assertViolates(AdrArchitectureTest.aspectsAreObservabilityOrAllowListed, "bad.aspect", "AuthorizationAspect");
    }

    @Test
    @DisplayName("허용 목록에 올린 애스펙트는 관측 자리 밖이어도 통과한다")
    void passesAllowListedAspect() {
        String aspect = FIXTURE + "bad.aspect.order.application.service.AuthorizationAspect";
        AdrArchitectureTest.aspectsResideIn(AdrArchitectureTest.OBSERVABILITY_ASPECTS, Set.of(aspect))
            .allowEmptyShould(false).check(importPackage("bad.aspect"));
    }

    @Test
    @DisplayName("대체 배치를 고르면 그 배치의 자리로 판정한다")
    void judgesByChosenLayout() {
        ArchRule altLayout = AdrArchitectureTest.eventListenerMethodsResideIn("..listener..", "..setup..");
        altLayout.allowEmptyShould(false).check(importPackage("alt"));
        assertViolates(altLayout, "altbad", "ReceiptListener.sendReceiptOnOrderPlaced");
    }

    private static void assertViolates(ArchRule rule, String fixture, String expectedInMessage) {
        AssertionError failure = assertThrows(AssertionError.class, () -> rule.check(importPackage(fixture)));
        assertTrue(failure.getMessage().contains(expectedInMessage),
            () -> "expected violation of " + expectedInMessage + " but was: " + failure.getMessage());
    }

    private static JavaClasses importPackage(String fixture) {
        return new ClassFileImporter().importPackages(FIXTURE + fixture);
    }
}
