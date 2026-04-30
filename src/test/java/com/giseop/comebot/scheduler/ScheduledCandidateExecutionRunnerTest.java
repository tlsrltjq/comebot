package com.giseop.comebot.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduledCandidateExecutionRunnerTest {

    @Test
    void runScheduledDoesNotExecuteWhenDisabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(false);
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService, never()).execute("KRW-BTC");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isZero();
    }

    @Test
    void runScheduledExecutesConfiguredMarketsWhenEnabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService).execute("KRW-ETH");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(summary.executedMarkets()).isEqualTo(2);
    }

    @Test
    void runScheduledDoesNotExecuteWhenMarketsAreEmpty() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of());
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService, never()).execute(org.mockito.ArgumentMatchers.anyString());
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isZero();
    }

    @Test
    void runScheduledSkipsBlankMarkets() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", " "));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService, never()).execute(" ");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(1);
    }

    @Test
    void marketFailureDoesNotStopNextMarket() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute("KRW-BTC")).thenThrow(new IllegalStateException("failed"));
        when(candidateExecutionService.execute("KRW-ETH")).thenReturn(result("KRW-ETH", SignalType.HOLD, null));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute("KRW-BTC");
        verify(candidateExecutionService).execute("KRW-ETH");
        org.assertj.core.api.Assertions.assertThat(summary.failedCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.holdCount()).isEqualTo(1);
    }

    @Test
    void runOnceSummarizesFilledRejectedAndHoldResults() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute("KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));
        when(candidateExecutionService.execute("KRW-ETH")).thenReturn(result("KRW-ETH", SignalType.BUY, OrderStatus.REJECTED));
        when(candidateExecutionService.execute("KRW-XRP")).thenReturn(result("KRW-XRP", SignalType.HOLD, null));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(summary.executedMarkets()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(summary.filledCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.rejectedCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.holdCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.failedCount()).isZero();
    }

    @Test
    void runScheduledSendsSummaryNotificationWhenSummaryExists() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute("KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));
        CandidateSchedulerNotificationService notificationService = mock(CandidateSchedulerNotificationService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService, notificationService).runScheduled();

        verify(notificationService).notifySummary(new CandidateSchedulerRunSummary(1, 1, 1, 0, 0, 0));
    }

    @Test
    void runScheduledDoesNotSendSummaryNotificationWhenNoMarketRuns() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of());
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        CandidateSchedulerNotificationService notificationService = mock(CandidateSchedulerNotificationService.class);

        new ScheduledCandidateExecutionRunner(properties, candidateExecutionService, notificationService).runScheduled();

        verify(notificationService, never()).notifySummary(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runOnceSummaryMatchesSavedHistoryForHandledResults() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        InMemoryTradingFlowHistoryRepository historyRepository = new InMemoryTradingFlowHistoryRepository();
        TradingFlowHistoryService historyService = new TradingFlowHistoryService(historyRepository);
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute("KRW-BTC"))
                .thenAnswer(invocation -> save(historyService, result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED)));
        when(candidateExecutionService.execute("KRW-ETH"))
                .thenAnswer(invocation -> save(historyService, result("KRW-ETH", SignalType.BUY, OrderStatus.REJECTED)));
        when(candidateExecutionService.execute("KRW-XRP"))
                .thenAnswer(invocation -> save(historyService, result("KRW-XRP", SignalType.HOLD, null)));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        org.assertj.core.api.Assertions.assertThat(historyRepository.findRecent(10)).hasSize(summary.executedMarkets());
        org.assertj.core.api.Assertions.assertThat(historyRepository.findRecent(10))
                .extracting(history -> history.orderStatus() == null ? "HOLD" : history.orderStatus().name())
                .containsExactlyInAnyOrder("FILLED", "REJECTED", "HOLD");
        org.assertj.core.api.Assertions.assertThat(summary.filledCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.rejectedCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.holdCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.failedCount()).isZero();
    }

    private TradingFlowResult result(String market, SignalType signalType, OrderStatus orderStatus) {
        return new TradingFlowResult(
                market,
                new BigDecimal("100"),
                signalType,
                "test",
                orderStatus != null,
                orderStatus,
                "message",
                Instant.now()
        );
    }

    private ScheduledCandidateExecutionRunner runner(
            CandidateSchedulerProperties properties,
            CandidateExecutionService candidateExecutionService
    ) {
        return new ScheduledCandidateExecutionRunner(
                properties,
                candidateExecutionService,
                mock(CandidateSchedulerNotificationService.class)
        );
    }

    private TradingFlowResult save(TradingFlowHistoryService historyService, TradingFlowResult result) {
        historyService.save(result);
        return result;
    }
}
