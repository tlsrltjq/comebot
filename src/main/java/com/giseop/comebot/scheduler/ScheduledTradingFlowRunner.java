package com.giseop.comebot.scheduler;

import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTradingFlowRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTradingFlowRunner.class);

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final TradingFlowService tradingFlowService;
    private final MarketSelectionService marketSelectionService;

    @Autowired
    public ScheduledTradingFlowRunner(
            TradingSchedulerProperties tradingSchedulerProperties,
            TradingFlowService tradingFlowService,
            MarketSelectionService marketSelectionService
    ) {
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.tradingFlowService = tradingFlowService;
        this.marketSelectionService = marketSelectionService;
    }

    ScheduledTradingFlowRunner(
            TradingSchedulerProperties tradingSchedulerProperties,
            TradingFlowService tradingFlowService
    ) {
        this(
                tradingSchedulerProperties,
                tradingFlowService,
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore())
        );
    }

    @Scheduled(fixedDelayString = "${trading.scheduler.fixed-delay-ms:60000}")
    public void runScheduled() {
        if (!tradingSchedulerProperties.isEnabled()) {
            return;
        }

        List<String> markets = marketSelectionService.resolve(tradingSchedulerProperties.getMarkets());
        if (markets.isEmpty()) {
            return;
        }

        try {
            tradingFlowService.runAll(markets);
        } catch (RuntimeException exception) {
            log.warn("Scheduled trading flow batch failed. markets={}, error={}", markets.size(), exception.getClass().getSimpleName());
        }
    }
}
