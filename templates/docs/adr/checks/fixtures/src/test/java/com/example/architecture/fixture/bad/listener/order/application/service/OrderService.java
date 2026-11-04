package com.example.architecture.fixture.bad.listener.order.application.service;

import org.springframework.transaction.event.TransactionalEventListener;

// 어긋남: 서비스 안에 리스너.
public class OrderService {

    @TransactionalEventListener
    public void sendReceiptOnOrderPlaced(Object event) {
    }
}
