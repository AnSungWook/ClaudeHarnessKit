package com.example.architecture.fixture.good.order.adapter.in.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

// ApplicationListener 구현도 adapter/in/event.
public class OrderPlacedApplicationListener implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
}
