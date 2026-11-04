package com.example.architecture.fixture.good.order.adapter.in.schedule;

import org.springframework.scheduling.annotation.Scheduled;

// 스케줄 트리거는 adapter/in/schedule. 같은 메서드에 두 번 달면 반복 선언 컨테이너 @Schedules 로 컴파일된다.
public class SettlementSchedule {

    @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(cron = "0 0 15 * * *")
    public void startDailySettlement() {
    }
}
