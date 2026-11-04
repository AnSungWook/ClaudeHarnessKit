package com.example.architecture.fixture.good.order.domain;

// 이벤트 타입은 발행한 기능의 domain 에 둔다(ADR-001, ADR-017).
public record OrderPlacedEvent(long orderId) {
}
