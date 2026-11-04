package com.example.architecture.fixture.bad.lifecycle.order.application.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

// 어긋남: 생명주기 리스너도 config 나 adapter/in/event 밖이면 걸린다.
public class StartupService {

    @EventListener
    public void warmUpOnReady(ApplicationReadyEvent event) {
    }
}
