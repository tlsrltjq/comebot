package com.giseop.comebot.scheduler;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCandidateExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCandidateExecutionRunner.class);

    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final CandidateExecutionService candidateExecutionService;
    private final CandidateSchedulerNotificationService candidateSchedulerNotificationService;
    private final MarketSelectionService marketSelectionService;
    private final MarketDataReadinessService marketDataReadinessService;
    private final AtomicBoolean running;
    private final AtomicLong lastRunStartedAt;
    private final AtomicLong lastMarketDataSkipLoggedAt;

    @Autowired
    public ScheduledCandidateExecutionRunner(
            CandidateSchedulerProperties candidateSchedulerProperties,
            CandidateExecutionService candidateExecutionService,
            CandidateSchedulerNotificationService candidateSchedulerNotificationService,
            MarketSelectionService marketSelectionService,
            MarketDataReadinessService marketDataReadinessService
    ) {
        this(candidateSchedulerProperties, candidateExecutionService, candidateSchedulerNotificationService, marketSelectionService, marketDataReadinessService, new AtomicBoolean(false));
    }

    ScheduledCandidateExecutionRunner(
            CandidateSchedulerProperties candidateSchedulerProperties,
            CandidateExecutionService candidateExecutionService,
            CandidateSchedulerNotificationService candidateSchedulerNotificationService,
            MarketSelectionService marketSelectionService,
            MarketDataReadinessService marketDataReadinessService,
            AtomicBoolean running
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.candidateExecutionService = candidateExecutionService;
        this.candidateSchedulerNotificationService = candidateSchedulerNotificationService;
        this.marketSelectionService = marketSelectionService;
        this.marketDataReadinessService = marketDataReadinessService;
        this.running = running;
        this.lastRunStartedAt = new AtomicLong(0);
        this.lastMarketDataSkipLoggedAt = new AtomicLong(0);
    }

    ScheduledCandidateExecutionRunner(
            CandidateSchedulerProperties candidateSchedulerProperties,
            CandidateExecutionService candidateExecutionService,
            CandidateSchedulerNotificationService candidateSchedulerNotificationService
    ) {
        this(
                candidateSchedulerProperties,
                candidateExecutionService,
                candidateSchedulerNotificationService,
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                null
        );
    }

    @Scheduled(fixedDelay = 30000)
    public void runScheduled() {
        CandidateSchedulerRunSummary summary = runScheduledIfDue();
        if (summary.requestedMarkets() > 0) {
            log.info(
                    "Scheduled candidate execution summary. requested={}, executed={}, filled={}, rejected={}, hold={}, failed={}",
                    summary.requestedMarkets(),
                    summary.executedMarkets(),
                    summary.filledCount(),
                    summary.rejectedCount(),
                    summary.holdCount(),
                    summary.failedCount()
            );
            candidateSchedulerNotificationService.notifySummary(summary);
        }
    }

    public CandidateSchedulerRunSummary runScheduledIfDue() {
        if (!isDue()) {
            return CandidateSchedulerRunSummary.empty();
        }
        return runOnce();
    }

    public CandidateSchedulerRunSummary runOnce() {
        if (!candidateSchedulerProperties.isEnabled()) {
            return CandidateSchedulerRunSummary.empty();
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled candidate execution skipped because previous run is still active");
            return CandidateSchedulerRunSummary.empty();
        }

        try {
            lastRunStartedAt.set(System.currentTimeMillis());
            return executeOnce();
        } finally {
            running.set(false);
        }
    }

    private boolean isDue() {
        long fixedDelayMs = Math.max(30000, candidateSchedulerProperties.getFixedDelayMs());
        long now = System.currentTimeMillis();
        long previous = lastRunStartedAt.get();
        return previous == 0 || now - previous >= fixedDelayMs;
    }

    private CandidateSchedulerRunSummary executeOnce() {
        CandidateSchedulerRunSummary summary = CandidateSchedulerRunSummary.empty();
        int requestedMarkets = 0;
        for (ExchangeMode exchange : candidateSchedulerProperties.getExchanges()) {
            MarketDataReadiness readiness = marketDataReadinessService == null ? null : marketDataReadinessService.readiness(exchange);
            if (readiness != null && !readiness.ready()) {
                logMarketDataSkip("Scheduled candidate execution", exchange, readiness);
                continue;
            }
            List<String> markets = marketSelectionService.resolve(exchange, candidateSchedulerProperties.getMarkets());
            requestedMarkets += markets.size();
            for (int index = 0; index < markets.size(); index++) {
                String market = markets.get(index);
                summary = summary.add(executeMarket(exchange, market));
                delayBeforeNextMarket(index, markets.size());
            }
        }
        return new CandidateSchedulerRunSummary(
                requestedMarkets,
                summary.executedMarkets(),
                summary.filledCount(),
                summary.rejectedCount(),
                summary.holdCount(),
                summary.failedCount()
        );
    }

    private CandidateSchedulerRunSummary executeMarket(ExchangeMode exchange, String market) {
        try {
            TradingFlowResult result = candidateExecutionService.execute(exchange, market);
            return summarizeResult(result);
        } catch (RuntimeException exception) {
            log.warn(
                    "Scheduled candidate execution failed. exchange={}, market={}, error={}",
                    exchange,
                    market,
                    exception.getClass().getSimpleName()
            );
            return new CandidateSchedulerRunSummary(0, 1, 0, 0, 0, 1);
        }
    }

    private CandidateSchedulerRunSummary summarizeResult(TradingFlowResult result) {
        if (result == null) {
            return new CandidateSchedulerRunSummary(0, 1, 0, 0, 0, 1);
        }
        if (result.orderStatus() == OrderStatus.FILLED) {
            return new CandidateSchedulerRunSummary(0, 1, 1, 0, 0, 0);
        }
        if (result.orderStatus() == OrderStatus.REJECTED) {
            return new CandidateSchedulerRunSummary(0, 1, 0, 1, 0, 0);
        }
        if (result.signalType() == SignalType.HOLD) {
            return new CandidateSchedulerRunSummary(0, 1, 0, 0, 1, 0);
        }
        return new CandidateSchedulerRunSummary(0, 1, 0, 0, 0, 1);
    }

    private void logMarketDataSkip(String schedulerName, ExchangeMode exchange, MarketDataReadiness readiness) {
        long now = System.currentTimeMillis();
        long previous = lastMarketDataSkipLoggedAt.get();
        if (previous != 0 && now - previous < 60000) {
            return;
        }
        if (!lastMarketDataSkipLoggedAt.compareAndSet(previous, now)) {
            return;
        }
        log.warn(
                "{} skipped because market data is not ready. exchange={}, reason={}, snapshots={}, freshSnapshots={}",
                schedulerName,
                exchange,
                readiness.reason(),
                readiness.snapshotCount(),
                readiness.freshSnapshotCount()
        );
    }

    private void delayBeforeNextMarket(int currentIndex, int marketCount) {
        long delayMs = candidateSchedulerProperties.getPerMarketDelayMs();
        if (delayMs <= 0 || currentIndex >= marketCount - 1) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Scheduled candidate execution delay interrupted");
        }
    }
}
