package com.giseop.comebot.scheduler;

import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
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
        if (!candidateSchedulerProperties.isEnabled()) {
            return;
        }

        candidateSchedulerProperties.getMarkets().stream()
                .filter(market -> market != null && !market.isBlank())
                .forEach(this::executeMarket);
    }

    private void executeMarket(String market) {
        try {
            candidateExecutionService.execute(market);
        } catch (RuntimeException exception) {
            log.warn("Scheduled candidate execution failed. market={}, error={}", market, exception.getClass().getSimpleName());
        }
    }
}
