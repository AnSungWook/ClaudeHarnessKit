package com.example.architecture.fixture.bad.applistener.order.application.service;

import com.example.architecture.fixture.good.order.adapter.in.event.BaseOrderEventListener;
import org.springframework.context.ApplicationEvent;

// 어긋남: 상속으로 ApplicationListener 를 구현한 클래스가 서비스 폴더에. 부모는 바른 자리라 이 자식만 걸려야 한다.
public class OrderListener extends BaseOrderEventListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }
}
