package com.giseop.comebot.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ScheduledCandidateExecutionRunnerTest {

    @Test
    void runScheduledDoesNotExecuteWhenDisabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(false);
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "KRW-BTC");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isZero();
    }

    @Test
    void runScheduledExecutesConfiguredMarketsWhenEnabled() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-ETH");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(summary.executedMarkets()).isEqualTo(2);
    }

    @Test
    void runScheduledUsesConfiguredExchange() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setExchange(ExchangeMode.BINANCE);
        properties.setMarkets(List.of("BTCUSDT"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.BINANCE, "BTCUSDT"))
                .thenReturn(result("BTCUSDT", SignalType.HOLD, null));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.BINANCE, "BTCUSDT");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.holdCount()).isEqualTo(1);
    }

    @Test
    void runScheduledCanExecuteUpbitAndBinanceInSameRun() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setExchanges(List.of(ExchangeMode.UPBIT, ExchangeMode.BINANCE));
        properties.setMarkets(List.of("KRW-BTC", "BTCUSDT"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC"))
                .thenReturn(result("KRW-BTC", SignalType.HOLD, null));
        when(candidateExecutionService.execute(ExchangeMode.BINANCE, "BTCUSDT"))
                .thenReturn(result("BTCUSDT", SignalType.HOLD, null));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService).execute(ExchangeMode.BINANCE, "BTCUSDT");
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "BTCUSDT");
        verify(candidateExecutionService, never()).execute(ExchangeMode.BINANCE, "KRW-BTC");
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

        verify(candidateExecutionService, never()).execute(
                org.mockito.ArgumentMatchers.any(ExchangeMode.class),
                org.mockito.ArgumentMatchers.anyString()
        );
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isZero();
    }

    @Test
    void runScheduledSkipsBlankMarkets() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", " "));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, " ");
        org.assertj.core.api.Assertions.assertThat(summary.requestedMarkets()).isEqualTo(1);
    }

    @Test
    void marketFailureDoesNotStopNextMarket() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC")).thenThrow(new IllegalStateException("failed"));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-ETH")).thenReturn(result("KRW-ETH", SignalType.HOLD, null));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-ETH");
        org.assertj.core.api.Assertions.assertThat(summary.failedCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(summary.holdCount()).isEqualTo(1);
    }

    @Test
    void runOnceSkipsWhenPreviousRunIsActive() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);

        CandidateSchedulerRunSummary summary = new ScheduledCandidateExecutionRunner(
                properties,
                candidateExecutionService,
                mock(CandidateSchedulerNotificationService.class),
                new com.giseop.comebot.market.service.MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                null,
                new AtomicBoolean(true)
        ).runOnce();

        org.assertj.core.api.Assertions.assertThat(summary).isEqualTo(CandidateSchedulerRunSummary.empty());
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "KRW-BTC");
    }

    @Test
    void runOnceSkipsExchangeWhenMarketDataIsNotReady() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setExchanges(List.of(ExchangeMode.UPBIT));
        properties.setMarkets(List.of("KRW-BTC"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        MarketDataReadinessService readinessService = mock(MarketDataReadinessService.class);
        when(readinessService.readiness(ExchangeMode.UPBIT))
                .thenReturn(MarketDataReadiness.snapshot(ExchangeMode.UPBIT, 0, 0));

        CandidateSchedulerRunSummary summary = new ScheduledCandidateExecutionRunner(
                properties,
                candidateExecutionService,
                mock(CandidateSchedulerNotificationService.class),
                new MarketSelectionService(new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                readinessService,
                new AtomicBoolean(false)
        ).runOnce();

        org.assertj.core.api.Assertions.assertThat(summary).isEqualTo(CandidateSchedulerRunSummary.empty());
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "KRW-BTC");
    }

    @Test
    void runOnceSummarizesFilledRejectedAndHoldResults() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-ETH")).thenReturn(result("KRW-ETH", SignalType.BUY, OrderStatus.REJECTED));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-XRP")).thenReturn(result("KRW-XRP", SignalType.HOLD, null));

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
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));
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
    void maxBuysPerRunLimitsFilledOrdersPerExchangeCycle() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMaxBuysPerRun(1);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "KRW-ETH");
        verify(candidateExecutionService, never()).execute(ExchangeMode.UPBIT, "KRW-XRP");
        org.assertj.core.api.Assertions.assertThat(summary.filledCount()).isEqualTo(1);
    }

    @Test
    void maxBuysPerRunZeroMeansUnlimited() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMaxBuysPerRun(0);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-ETH")).thenReturn(result("KRW-ETH", SignalType.BUY, OrderStatus.FILLED));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-XRP")).thenReturn(result("KRW-XRP", SignalType.BUY, OrderStatus.FILLED));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-BTC");
        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-ETH");
        verify(candidateExecutionService).execute(ExchangeMode.UPBIT, "KRW-XRP");
        org.assertj.core.api.Assertions.assertThat(summary.filledCount()).isEqualTo(3);
    }

    @Test
    void runOnceSummaryMatchesSavedHistoryForHandledResults() {
        CandidateSchedulerProperties properties = new CandidateSchedulerProperties();
        properties.setEnabled(true);
        properties.setMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        InMemoryTradingFlowHistoryRepository historyRepository = new InMemoryTradingFlowHistoryRepository();
        TradingFlowHistoryService historyService = new TradingFlowHistoryService(historyRepository);
        CandidateExecutionService candidateExecutionService = mock(CandidateExecutionService.class);
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-BTC"))
                .thenAnswer(invocation -> save(historyService, result("KRW-BTC", SignalType.BUY, OrderStatus.FILLED)));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-ETH"))
                .thenAnswer(invocation -> save(historyService, result("KRW-ETH", SignalType.BUY, OrderStatus.REJECTED)));
        when(candidateExecutionService.execute(ExchangeMode.UPBIT, "KRW-XRP"))
                .thenAnswer(invocation -> save(historyService, result("KRW-XRP", SignalType.HOLD, null)));

        CandidateSchedulerRunSummary summary = runner(properties, candidateExecutionService).runOnce();

        org.assertj.core.api.Assertions.assertThat(historyRepository.findRecent(10)).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(historyRepository.findRecent(10))
                .extracting(history -> history.orderStatus().name())
                .containsExactlyInAnyOrder("FILLED", "REJECTED");
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
