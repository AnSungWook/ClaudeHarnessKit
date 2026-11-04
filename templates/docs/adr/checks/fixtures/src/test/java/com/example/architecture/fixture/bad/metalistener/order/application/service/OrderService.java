package com.example.architecture.fixture.bad.metalistener.order.application.service;

import com.example.architecture.fixture.good.order.adapter.in.event.OrderEventHandler;

// 어긋남: 합성 어노테이션으로 단 리스너도 서비스에 두면 걸린다.
public class OrderService {

    @OrderEventHandler
    public void recordPointOnOrderPlaced(Object event) {
    }
}
