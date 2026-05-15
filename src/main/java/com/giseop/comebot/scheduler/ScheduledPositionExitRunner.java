package com.giseop.comebot.scheduler;

import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.trading.service.PositionExitExecutionService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPositionExitRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPositionExitRunner.class);

    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final PositionExitExecutionService positionExitExecutionService;
    private final MarketDataReadinessService marketDataReadinessService;
    private final AtomicBoolean running;
    private final AtomicLong lastMarketDataSkipLoggedAt;

    @Autowired
    public ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService,
            MarketDataReadinessService marketDataReadinessService
    ) {
        this(positionExitSchedulerProperties, positionExitExecutionService, marketDataReadinessService, new AtomicBoolean(false));
    }

    ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService,
            MarketDataReadinessService marketDataReadinessService,
            AtomicBoolean running
    ) {
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.positionExitExecutionService = positionExitExecutionService;
        this.marketDataReadinessService = marketDataReadinessService;
        this.running = running;
        this.lastMarketDataSkipLoggedAt = new AtomicLong(0);
    }

    ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService,
            AtomicBoolean running
    ) {
        this(positionExitSchedulerProperties, positionExitExecutionService, null, running);
    }

    ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService
    ) {
        this(positionExitSchedulerProperties, positionExitExecutionService, null, new AtomicBoolean(false));
    }

    @Scheduled(fixedDelayString = "${trading.exit-scheduler.fixed-delay-ms:5000}")
    public void runScheduled() {
        runOnce();
    }

    public PositionExitRunSummary runOnce() {
        if (!positionExitSchedulerProperties.isEnabled()) {
            return PositionExitRunSummary.empty();
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled position exit skipped because previous run is still active");
            return PositionExitRunSummary.empty();
        }
        try {
            PositionExitRunSummary summary = PositionExitRunSummary.empty();
            for (var exchange : positionExitSchedulerProperties.getExchanges()) {
                MarketDataReadiness readiness = marketDataReadinessService == null ? null : marketDataReadinessService.readiness(exchange);
                if (readiness != null && !readiness.ready()) {
                    logMarketDataSkip(exchange, readiness);
                    continue;
                }
                summary = summary.add(positionExitExecutionService.execute(exchange));
            }
            if (summary.positionMarkets() > 0) {
                log.info(
                        "Scheduled position exit summary. positions={}, evaluated={}, sold={}, rejected={}, hold={}, failed={}",
                        summary.positionMarkets(),
                        summary.evaluatedMarkets(),
                        summary.soldCount(),
                        summary.rejectedCount(),
                        summary.holdCount(),
                        summary.failedCount()
                );
            }
            return summary;
        } catch (RuntimeException exception) {
            log.warn("Scheduled position exit failed. error={}", exception.getClass().getSimpleName());
            return new PositionExitRunSummary(0, 1, 0, 0, 0, 1);
        } finally {
            running.set(false);
        }
    }

    private void logMarketDataSkip(com.giseop.comebot.exchange.ExchangeMode exchange, MarketDataReadiness readiness) {
        long now = System.currentTimeMillis();
        long previous = lastMarketDataSkipLoggedAt.get();
        if (previous != 0 && now - previous < 60000) {
            return;
        }
        if (!lastMarketDataSkipLoggedAt.compareAndSet(previous, now)) {
            return;
        }
        log.warn(
                "Scheduled position exit skipped because market data is not ready. exchange={}, reason={}, snapshots={}, freshSnapshots={}",
                exchange,
                readiness.reason(),
                readiness.snapshotCount(),
                readiness.freshSnapshotCount()
        );
    }
}
