package com.example.architecture.fixture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 메시지 라이브러리 없이 클래스 수준 수신 어노테이션을 흉내 낸 fixture 용 어노테이션(예: 클래스에 단 @KafkaListener).
// fixture 테스트는 이 이름과 컨테이너 이름을 입구 목록으로 넘겨 규칙을 만든다.
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FixtureMessageListeners.class)
public @interface FixtureMessageListener {
    String value() default "";
}
