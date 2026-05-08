package com.giseop.comebot.scheduler.controller;

import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.scheduler.dto.SchedulerStatusResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchedulerStatusController {

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final PaperPortfolioService paperPortfolioService;

    public SchedulerStatusController(
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PaperPortfolioService paperPortfolioService
    ) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.paperPortfolioService = paperPortfolioService;
    }

    @GetMapping("/api/scheduler/status")
    public SchedulerStatusResponse getStatus() {
        return new SchedulerStatusResponse(
                tradingSchedulerProperties.isEnabled(),
                tradingSchedulerProperties.getFixedDelayMs(),
                tradingSchedulerProperties.getMarkets(),
                candidateSchedulerProperties.isEnabled(),
                candidateSchedulerProperties.getFixedDelayMs(),
                candidateSchedulerProperties.getMarkets(),
                candidateSchedulerProperties.isNotifySummary(),
                candidateSchedulerProperties.getExchange().name(),
                positionExitSchedulerProperties.isEnabled(),
                positionExitSchedulerProperties.getFixedDelayMs(),
                positionExitSchedulerProperties.isSaveHoldHistory(),
                positionExitSchedulerProperties.getExchange().name(),
                positionMarketCount()
        );
    }

    private int positionMarketCount() {
        return (int) paperPortfolioService.findPositions(positionExitSchedulerProperties.getExchange()).stream()
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .map(position -> position.market())
                .filter(market -> market != null && !market.isBlank())
                .distinct()
                .count();
    }
}
