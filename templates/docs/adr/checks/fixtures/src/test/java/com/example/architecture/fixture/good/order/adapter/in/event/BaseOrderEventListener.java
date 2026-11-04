package com.example.architecture.fixture.good.order.adapter.in.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

// 바른 자리의 추상 부모. bad.applistener 의 자식이 이것을 상속해, 자식만 잘못된 자리에 있을 때 잡히는지 본다.
public abstract class BaseOrderEventListener implements ApplicationListener<ApplicationEvent> {
}
