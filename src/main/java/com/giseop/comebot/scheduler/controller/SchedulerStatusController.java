package com.giseop.comebot.scheduler.controller;

import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.scheduler.SchedulerControlService;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.scheduler.dto.SchedulerStatusResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchedulerStatusController {

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final SchedulerControlService schedulerControlService;
    private final PaperPortfolioService paperPortfolioService;

    public SchedulerStatusController(
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            SchedulerControlService schedulerControlService,
            PaperPortfolioService paperPortfolioService
    ) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.schedulerControlService = schedulerControlService;
        this.paperPortfolioService = paperPortfolioService;
    }

    @GetMapping("/api/scheduler/status")
    public SchedulerStatusResponse getStatus() {
        return status();
    }

    @PutMapping("/api/scheduler/control")
    public ResponseEntity<SchedulerStatusResponse> updateControl(@RequestBody SchedulerControlRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            schedulerControlService.update(request.autoTradingEnabled(), request.candidateFixedDelayMs());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(status());
    }

    private SchedulerStatusResponse status() {
        return new SchedulerStatusResponse(
                tradingSchedulerProperties.isEnabled(),
                tradingSchedulerProperties.getFixedDelayMs(),
                tradingSchedulerProperties.getMarkets(),
                candidateSchedulerProperties.isEnabled(),
                candidateSchedulerProperties.getFixedDelayMs(),
                candidateSchedulerProperties.getMarkets(),
                candidateSchedulerProperties.isNotifySummary(),
                candidateSchedulerProperties.getExchange().name(),
                names(candidateSchedulerProperties.getExchanges()),
                positionExitSchedulerProperties.isEnabled(),
                positionExitSchedulerProperties.getFixedDelayMs(),
                positionExitSchedulerProperties.isSaveHoldHistory(),
                positionExitSchedulerProperties.getExchange().name(),
                names(positionExitSchedulerProperties.getExchanges()),
                positionMarketCount()
        );
    }

    private int positionMarketCount() {
        return positionExitSchedulerProperties.getExchanges().stream()
                .mapToInt(exchange -> (int) paperPortfolioService.findPositions(exchange).stream()
                        .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                        .map(position -> position.market())
                        .filter(market -> market != null && !market.isBlank())
                        .distinct()
                        .count())
                .sum();
    }

    private List<String> names(List<com.giseop.comebot.exchange.ExchangeMode> exchanges) {
        return exchanges.stream()
                .map(com.giseop.comebot.exchange.ExchangeMode::name)
                .toList();
    }

    public record SchedulerControlRequest(
            Boolean autoTradingEnabled,
            Long candidateFixedDelayMs
    ) {
    }
}
