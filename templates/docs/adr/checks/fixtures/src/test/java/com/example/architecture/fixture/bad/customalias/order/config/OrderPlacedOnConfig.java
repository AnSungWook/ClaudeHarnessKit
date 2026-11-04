package com.example.architecture.fixture.bad.customalias.order.config;

import com.example.architecture.fixture.good.order.domain.OrderPlacedEvent;

// 어긋남: 업무 이벤트 리스너가 config 에. 합성 어노테이션의 별칭 아닌 value(ApplicationReadyEvent)로 생명주기 예외를 얻으면 안 된다.
public class OrderPlacedOnConfig {

    @CustomListener
    public void recordOrderOnOrderPlaced(OrderPlacedEvent event) {
    }
}
