package com.giseop.comebot.scheduler;

import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled runner for the legacy {@link com.giseop.comebot.trading.service.TradingFlowService}.
 * Disabled by default via {@code trading.scheduler.enabled=false} (ADR-007).
 *
 * @deprecated Use {@link ScheduledCandidateExecutionRunner} and
 *             {@link ScheduledPositionExitRunner} instead.
 */
@Deprecated(since = "2026-05-22")
@Component
public class ScheduledTradingFlowRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTradingFlowRunner.class);

    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final TradingFlowService tradingFlowService;
    private final MarketSelectionService marketSelectionService;
    private final AtomicBoolean running = new AtomicBoolean(false);

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
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled trading flow skipped because previous run is still active");
            return;
        }

        List<String> markets = List.of();
        try {
            markets = marketSelectionService.resolve(tradingSchedulerProperties.getMarkets());
            if (markets.isEmpty()) {
                return;
            }

            tradingFlowService.runAll(markets);
        } catch (RuntimeException exception) {
            log.warn("Scheduled trading flow batch failed. markets={}, error={}", markets.size(), exception.getClass().getSimpleName());
        } finally {
            running.set(false);
        }
    }
}
