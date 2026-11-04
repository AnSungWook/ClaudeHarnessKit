package com.example.architecture.fixture.good.order.adapter.in.event;

import com.example.architecture.fixture.good.order.domain.OrderPlacedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

// 바른 자리: 리스너는 adapter/in/event. @TransactionalEventListener 는 @EventListener 를 메타 어노테이션으로 든다.
public class ReceiptListener {

    @TransactionalEventListener
    public void sendReceiptOnOrderPlaced(OrderPlacedEvent event) {
    }

    @OrderEventHandler
    public void recordPointOnOrderPlaced(OrderPlacedEvent event) {
    }

    @EventListener
    public void countOrderOnOrderPlaced(OrderPlacedEvent event) {
    }
}
