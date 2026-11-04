package com.example.architecture.fixture.good.order.adapter.in.message;

import com.example.architecture.fixture.FixtureMessageListener;

// 바른 자리: 클래스 수준 수신 선언은 adapter/in/message.
@FixtureMessageListener("orders")
public class OrderMessages {
}
