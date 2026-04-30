package com.giseop.comebot.scheduler.controller;

import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.scheduler.dto.SchedulerStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchedulerStatusController {

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;

    public SchedulerStatusController(
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties
    ) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
    }

    @GetMapping("/api/scheduler/status")
    public SchedulerStatusResponse getStatus() {
        return new SchedulerStatusResponse(
                tradingSchedulerProperties.isEnabled(),
                tradingSchedulerProperties.getFixedDelayMs(),
                tradingSchedulerProperties.getMarkets(),
                candidateSchedulerProperties.isEnabled(),
                candidateSchedulerProperties.getFixedDelayMs(),
                candidateSchedulerProperties.getMarkets()
        );
    }
}
