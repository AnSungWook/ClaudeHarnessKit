package com.example.architecture.fixture.alt.order.listener;

import org.springframework.context.event.EventListener;

// 대체 배치(리스너 자리를 ..listener.. 로 고른 저장소)에서 바른 자리.
public class ReceiptListener {

    @EventListener
    public void sendReceiptOnOrderPlaced(Object event) {
    }
}
