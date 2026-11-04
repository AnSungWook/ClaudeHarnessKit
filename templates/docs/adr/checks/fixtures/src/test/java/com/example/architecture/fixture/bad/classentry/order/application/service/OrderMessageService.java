package com.example.architecture.fixture.bad.classentry.order.application.service;

import com.example.architecture.fixture.FixtureMessageListener;

// 어긋남: 클래스 수준 수신 선언이 서비스에.
@FixtureMessageListener("orders")
public class OrderMessageService {
}
