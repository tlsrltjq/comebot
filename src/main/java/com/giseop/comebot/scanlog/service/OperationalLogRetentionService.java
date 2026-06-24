package com.giseop.comebot.scanlog.service;

import com.giseop.comebot.scanlog.repository.OperationalLogRetentionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(OperationalLogRetentionService.class);

    private final OperationalLogRetentionRepository repository;
    private final Clock clock;
    private final boolean enabled;
    private final Duration candidateDetailRetention;
    private final Duration tradingFlowRetention;

    @Autowired
    public OperationalLogRetentionService(
            OperationalLogRetentionRepository repository,
            @Value("${log-retention.enabled:true}") boolean enabled,
            @Value("${log-retention.candidate-detail-retention-days:30}") long candidateDetailRetentionDays,
            @Value("${log-retention.trading-flow-retention-days:90}") long tradingFlowRetentionDays
    ) {
        this(
                repository,
                Clock.systemUTC(),
                enabled,
                Duration.ofDays(candidateDetailRetentionDays),
                Duration.ofDays(tradingFlowRetentionDays)
        );
    }

    OperationalLogRetentionService(
            OperationalLogRetentionRepository repository,
            Clock clock,
            boolean enabled,
            Duration candidateDetailRetention,
            Duration tradingFlowRetention
    ) {
        this.repository = repository;
        this.clock = clock;
        this.enabled = enabled;
        this.candidateDetailRetention = requirePositive(candidateDetailRetention, "candidateDetailRetention");
        this.tradingFlowRetention = requirePositive(tradingFlowRetention, "tradingFlowRetention");
    }

    @Scheduled(cron = "${log-retention.cron:0 10 3 * * *}", zone = "${log-retention.zone:UTC}")
    public void runScheduledRetention() {
        runRetention();
    }

    @Transactional
    public RetentionResult runRetention() {
        if (!enabled) {
            log.debug("Operational log retention skipped because log-retention.enabled=false");
            return RetentionResult.disabled();
        }

        Instant now = clock.instant();
        Instant candidateCutoff = now.minus(candidateDetailRetention);
        Instant tradingFlowCutoff = now.minus(tradingFlowRetention);

        int summarizedCandidateRows = repository.summarizeCandidateScansBefore(candidateCutoff);
        int deletedCandidateRows = repository.deleteCandidateScansBefore(candidateCutoff);
        int deletedTradingFlowRows = repository.deleteTradingFlowHistoryBefore(tradingFlowCutoff);

        RetentionResult result = new RetentionResult(
                true,
                candidateCutoff,
                tradingFlowCutoff,
                summarizedCandidateRows,
                deletedCandidateRows,
                deletedTradingFlowRows
        );
        log.info(
                "Operational log retention completed. candidateCutoff={}, summarizedCandidates={}, "
                        + "deletedCandidates={}, tradingFlowCutoff={}, deletedTradingFlow={}",
                candidateCutoff,
                summarizedCandidateRows,
                deletedCandidateRows,
                tradingFlowCutoff,
                deletedTradingFlowRows
        );
        return result;
    }

    private static Duration requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    public record RetentionResult(
            boolean enabled,
            Instant candidateCutoff,
            Instant tradingFlowCutoff,
            int summarizedCandidateRows,
            int deletedCandidateRows,
            int deletedTradingFlowRows
    ) {

        private static RetentionResult disabled() {
            return new RetentionResult(false, null, null, 0, 0, 0);
        }
    }
}
