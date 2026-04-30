package com.giseop.comebot.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduledCandidateExecutionRunnerTest {

    @Test
    void runScheduledDoesNotExecuteWhenDisabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(false);
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService).runScheduled();

        verify(candidateExecutionService, never()).execute("KRW-BTC");
    }

    @Test
    void runScheduledExecutesConfiguredMarketsWhenEnabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService).runScheduled();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService).execute("KRW-ETH");
    }

    @Test
    void runScheduledDoesNotExecuteWhenMarketsAreEmpty() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of());
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService).runScheduled();

        verify(candidateExecutionService, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void runScheduledSkipsBlankMarkets() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", " "));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService).runScheduled();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService, never()).execute(" ");
    }

    @Test
    void marketFailureDoesNotStopNextMarket() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute("KRW-BTC")).thenThrow(new IllegalStateException("failed"));

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService).runScheduled();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService).execute("KRW-ETH");
    }
}
