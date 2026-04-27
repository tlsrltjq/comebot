package com.giseop.comebot.scheduler;

import com.giseop.comebot.trading.service.TradingFlowService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTradingFlowRunner {

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final TradingFlowService tradingFlowService;

    public ScheduledTradingFlowRunner(
            TradingSchedulerProperties tradingSchedulerProperties,
            TradingFlowService tradingFlowService
    ) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.tradingFlowService = tradingFlowService;
    }

    @Scheduled(fixedDelayString = "${trading.scheduler.fixed-delay-ms:60000}")
    public void runScheduled() {
        if (!tradingSchedulerProperties.isEnabled()) {
            return;
        }

        tradingSchedulerProperties.getMarkets().stream()
                .filter(market -> market != null && !market.isBlank())
                .forEach(tradingFlowService::run);
    }
}
