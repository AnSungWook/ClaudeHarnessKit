package com.example.architecture.fixture.bad.aliasoverride.order.config;

import com.example.architecture.fixture.good.order.domain.OrderPlacedEvent;

// 어긋남: 별칭으로 고정값을 업무 이벤트로 덮어썼다. 실제로는 OrderPlacedEvent 를 받는 업무 리스너가 config 에 있다.
public class OrderPlacedOverrideOnConfig {

    @OverridingListener(events = OrderPlacedEvent.class)
    public void recordOrderOnOrderPlaced(Object event) {
    }
}
