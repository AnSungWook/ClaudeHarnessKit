package com.example.architecture.fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// FixtureMessageListener 를 메타 어노테이션으로 든 합성 어노테이션.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FixtureMessageListener("orders")
public @interface OrderTopicListener {
}
