package com.example.architecture.fixture.bad.schedule.order.application.service;

import org.springframework.scheduling.annotation.Scheduled;

// 어긋남: 서비스 안의 스케줄 트리거.
public class SettlementService {

    @Scheduled(cron = "0 0 3 * * *")
    public void startDailySettlement() {
    }
}
