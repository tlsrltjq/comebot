package com.giseop.comebot.scheduler;

import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCandidateExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCandidateExecutionRunner.class);

    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final CandidateExecutionService candidateExecutionService;

    public ScheduledCandidateExecutionRunner(
            CandidateSchedulerProperties candidateSchedulerProperties,
            CandidateExecutionService candidateExecutionService
    ) {
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.candidateExecutionService = candidateExecutionService;
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
        }
    }

    public CandidateSchedulerRunSummary runOnce() {
        if (!candidateSchedulerProperties.isEnabled()) {
            return CandidateSchedulerRunSummary.empty();
        }

        List<String> markets = candidateSchedulerProperties.getMarkets().stream()
                .filter(market -> market != null && !market.isBlank())
                .toList();

        CandidateSchedulerRunSummary summary = CandidateSchedulerRunSummary.empty();
        for (String market : markets) {
            summary = summary.add(executeMarket(market));
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

    private CandidateSchedulerRunSummary executeMarket(String market) {
        try {
            TradingFlowResult result = candidateExecutionService.execute(market);
            return summarizeResult(result);
        } catch (RuntimeException exception) {
            log.warn("Scheduled candidate execution failed. market={}, error={}", market, exception.getClass().getSimpleName());
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
}
