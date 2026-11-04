package com.example.architecture.fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// FixtureMessageListener 의 반복 선언 컨테이너.
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FixtureMessageListeners {
    FixtureMessageListener[] value();
}
