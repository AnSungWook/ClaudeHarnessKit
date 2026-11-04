package com.example.architecture.fixture.bad.customalias.order.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

// value 가 @AliasFor 로 @EventListener 에 이어지지 않았다 - Spring 은 이것을 이벤트 타입으로 읽지 않는다.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener
public @interface CustomListener {
    Class<?>[] value() default ApplicationReadyEvent.class;
}
