package com.giseop.comebot.scanlog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.scanlog.repository.OperationalLogRetentionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationalLogRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-23T09:00:00Z");

    private final RecordingRepository repository = new RecordingRepository();

    @Test
    void runRetentionSummarizesAndPrunesByConfiguredCutoffs() {
        repository.summarized = 7;
        repository.deletedCandidates = 5;
        repository.deletedTradingFlow = 3;
        OperationalLogRetentionService service = service(true, Duration.ofDays(30), Duration.ofDays(90));

        OperationalLogRetentionService.RetentionResult result = service.runRetention();

        assertThat(result.enabled()).isTrue();
        assertThat(result.candidateCutoff()).isEqualTo(Instant.parse("2026-05-24T09:00:00Z"));
        assertThat(result.tradingFlowCutoff()).isEqualTo(Instant.parse("2026-03-25T09:00:00Z"));
        assertThat(result.summarizedCandidateRows()).isEqualTo(7);
        assertThat(result.deletedCandidateRows()).isEqualTo(5);
        assertThat(result.deletedTradingFlowRows()).isEqualTo(3);
        assertThat(repository.summarizeCutoff).isEqualTo(result.candidateCutoff());
        assertThat(repository.deleteCandidateCutoff).isEqualTo(result.candidateCutoff());
        assertThat(repository.deleteTradingFlowCutoff).isEqualTo(result.tradingFlowCutoff());
    }

    @Test
    void runRetentionDoesNothingWhenDisabled() {
        OperationalLogRetentionService service = service(false, Duration.ofDays(30), Duration.ofDays(90));

        OperationalLogRetentionService.RetentionResult result = service.runRetention();

        assertThat(result.enabled()).isFalse();
        assertThat(repository.calls).isZero();
    }

    @Test
    void constructorRejectsNonPositiveRetention() {
        assertThatThrownBy(() -> service(true, Duration.ZERO, Duration.ofDays(90)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidateDetailRetention");
        assertThatThrownBy(() -> service(true, Duration.ofDays(30), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tradingFlowRetention");
    }

    private OperationalLogRetentionService service(
            boolean enabled,
            Duration candidateDetailRetention,
            Duration tradingFlowRetention
    ) {
        return new OperationalLogRetentionService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC),
                enabled,
                candidateDetailRetention,
                tradingFlowRetention
        );
    }

    private static final class RecordingRepository implements OperationalLogRetentionRepository {

        private int summarized;
        private int deletedCandidates;
        private int deletedTradingFlow;
        private int calls;
        private Instant summarizeCutoff;
        private Instant deleteCandidateCutoff;
        private Instant deleteTradingFlowCutoff;

        @Override
        public int summarizeCandidateScansBefore(Instant cutoff) {
            calls++;
            summarizeCutoff = cutoff;
            return summarized;
        }

        @Override
        public int deleteCandidateScansBefore(Instant cutoff) {
            calls++;
            deleteCandidateCutoff = cutoff;
            return deletedCandidates;
        }

        @Override
        public int deleteTradingFlowHistoryBefore(Instant cutoff) {
            calls++;
            deleteTradingFlowCutoff = cutoff;
            return deletedTradingFlow;
        }
    }
}
