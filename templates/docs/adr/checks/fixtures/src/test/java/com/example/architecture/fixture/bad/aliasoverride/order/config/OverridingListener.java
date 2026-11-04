package com.example.architecture.fixture.bad.aliasoverride.order.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

// 메타 어노테이션에 생명주기 이벤트를 고정해 두고, 별칭 속성으로 덮어쓸 수 있게 한 합성 어노테이션.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener(ApplicationReadyEvent.class)
public @interface OverridingListener {

    @AliasFor(annotation = EventListener.class, attribute = "classes")
    Class<?>[] events() default {};
}
