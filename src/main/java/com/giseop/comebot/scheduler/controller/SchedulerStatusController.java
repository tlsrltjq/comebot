package com.giseop.comebot.scheduler.controller;

import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.scheduler.dto.SchedulerStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchedulerStatusController {

    private final TradingSchedulerProperties tradingSchedulerProperties;

    public SchedulerStatusController(TradingSchedulerProperties tradingSchedulerProperties) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
    }

    @GetMapping("/api/scheduler/status")
    public SchedulerStatusResponse getStatus() {
        return new SchedulerStatusResponse(
                tradingSchedulerProperties.isEnabled(),
                tradingSchedulerProperties.getFixedDelayMs(),
                tradingSchedulerProperties.getMarkets()
        );
    }
}
