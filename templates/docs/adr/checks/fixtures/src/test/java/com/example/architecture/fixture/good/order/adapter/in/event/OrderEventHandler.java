package com.example.architecture.fixture.good.order.adapter.in.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.event.EventListener;

// 합성 어노테이션. 검사는 메타 어노테이션까지 따라간다.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EventListener
public @interface OrderEventHandler {
}
