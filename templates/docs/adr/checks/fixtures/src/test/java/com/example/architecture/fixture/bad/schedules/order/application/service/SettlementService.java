package com.example.architecture.fixture.bad.schedules.order.application.service;

import org.springframework.scheduling.annotation.Scheduled;

// 어긋남: 반복 선언(@Schedules 컨테이너)으로 단 트리거도 걸린다.
public class SettlementService {

    @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(cron = "0 0 15 * * *")
    public void startDailySettlement() {
    }
}
