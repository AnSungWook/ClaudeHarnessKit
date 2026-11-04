package com.example.architecture.fixture.good.order.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;

// 생명주기 이벤트만 받는 리스너는 config 에 둘 수 있다.
// 모양: 매개변수, 어노테이션 값(매개변수 없음), 어노테이션 값 + 넓은 매개변수, 합성 어노테이션에 고정한 값, ApplicationListener 타입 인자.
public class StartupLogListener implements ApplicationListener<ApplicationReadyEvent> {

    @EventListener
    public void logReadyByParameter(ApplicationReadyEvent event) {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logReadyByAnnotation() {
    }

    @EventListener(classes = ApplicationReadyEvent.class)
    public void logReadyByAnnotationWithWideParameter(Object event) {
    }

    @OnApplicationReady
    public void logReadyByComposedAnnotation() {
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
    }
}
