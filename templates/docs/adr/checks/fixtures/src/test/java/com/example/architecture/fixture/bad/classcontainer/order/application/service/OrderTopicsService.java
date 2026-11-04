package com.example.architecture.fixture.bad.classcontainer.order.application.service;

import com.example.architecture.fixture.FixtureMessageListener;

// 어긋남: 반복 선언(컨테이너로 컴파일됨)으로 단 클래스 수준 수신 선언이 서비스에.
@FixtureMessageListener("orders")
@FixtureMessageListener("refunds")
public class OrderTopicsService {
}
