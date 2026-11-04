package com.example.architecture.fixture.altbad.order.adapter.in.event;

import org.springframework.context.event.EventListener;

// 대체 배치(..listener..)를 고른 저장소에서는 기본 배치 자리(adapter/in/event)가 어긋난 자리다.
public class ReceiptListener {

    @EventListener
    public void sendReceiptOnOrderPlaced(Object event) {
    }
}
