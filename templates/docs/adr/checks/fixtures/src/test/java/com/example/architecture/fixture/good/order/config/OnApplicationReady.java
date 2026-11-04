package com.example.architecture.fixture.good.order.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

// 합성 어노테이션 안에 생명주기 이벤트 타입을 고정한 모양.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(ApplicationReadyEvent.class)
public @interface OnApplicationReady {
}
