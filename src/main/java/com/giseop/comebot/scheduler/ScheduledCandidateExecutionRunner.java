package com.giseop.comebot.scheduler;

import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicBoolean running;

    @Autowired
    public ScheduledCandidateExecutionRunner(
            CandidateSchedulerProperties candidateSchedulerProperties,
            CandidateExecutionService candidateExecutionService,
            CandidateSchedulerNotificationService candidateSchedulerNotificationService,
            MarketSelectionService marketSelectionService
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.candidateExecutionService = candidateExecutionService;
        this.candidateSchedulerNotificationService = candidateSchedulerNotificationService;
        this.marketSelectionService = marketSelectionService;
        this.running = new AtomicBoolean(false);
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
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore())
        );
    }

    @Scheduled(fixedDelayString = "${trading.candidate-scheduler.fixed-delay-ms:60000}")
    public void runScheduled() {
        CandidateSchedulerRunSummary summary = runOnce();
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

    public CandidateSchedulerRunSummary runOnce() {
        if (!candidateSchedulerProperties.isEnabled()) {
            return CandidateSchedulerRunSummary.empty();
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled candidate execution skipped because previous run is still active");
            return CandidateSchedulerRunSummary.empty();
        }

        try {
            return executeOnce();
        } finally {
            running.set(false);
        }
    }

    private CandidateSchedulerRunSummary executeOnce() {
        ExchangeMode exchange = candidateSchedulerProperties.getExchange();
        List<String> markets = marketSelectionService.resolve(candidateSchedulerProperties.getMarkets());

        CandidateSchedulerRunSummary summary = CandidateSchedulerRunSummary.empty();
        for (int index = 0; index < markets.size(); index++) {
            String market = markets.get(index);
            summary = summary.add(executeMarket(exchange, market));
            delayBeforeNextMarket(index, markets.size());
        }
        return new CandidateSchedulerRunSummary(
                markets.size(),
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
