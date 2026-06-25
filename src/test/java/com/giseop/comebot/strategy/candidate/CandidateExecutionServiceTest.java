package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.PendingLimitOrderService;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.service.PositionEntryGuardService;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidateExecutionServiceTest {

    @Mock
    private CandidateScannerService candidateScannerService;
    @Mock
    private PendingLimitOrderService pendingLimitOrderService;
    @Mock
    private TradingFlowHistoryService tradingFlowHistoryService;
    @Mock
    private NotificationPolicyService notificationPolicyService;
    @Mock
    private TradingFlowNotificationService tradingFlowNotificationService;
    @Mock
    private CandidateScanLogService candidateScanLogService;
    @Mock
    private KillSwitchService killSwitchService;
    @Mock
    private PositionEntryGuardService positionEntryGuardService;

    private final StrategyProperties strategyProperties = new StrategyProperties();
    private final NotificationProperties notificationProperties = new NotificationProperties();
    private CandidateExecutionService service;

    @BeforeEach
    void setUp() {
        strategyProperties.setOrderQuantity(new BigDecimal("0.01"));
        strategyProperties.setOrderAmount(new BigDecimal("10000"));
        service = new CandidateExecutionService(
                candidateScannerService,
                strategyMarketSettingsService(),
                tradingFlowHistoryService,
                candidateScanLogService,
                notificationProperties,
                notificationPolicyService,
                tradingFlowNotificationService,
                killSwitchService,
                positionEntryGuardService,
                pendingLimitOrderService
        );
    }

    @Test
    void selectedCandidatePlacesLimitOrder() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(), any(), any()))
                .thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(com.giseop.comebot.strategy.domain.SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(result.currentPrice()).isEqualByComparingTo("100");

        ArgumentCaptor<BigDecimal> priceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(pendingLimitOrderService).tryPlace(
                eq(ExchangeMode.UPBIT), eq("KRW-BTC"),
                priceCaptor.capture(), qtyCaptor.capture(), any());
        assertThat(priceCaptor.getValue()).isEqualByComparingTo("100");
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void skippedCandidateDoesNotPlaceLimitOrder() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(skippedCandidate());

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate was not selected");
        verify(pendingLimitOrderService, never()).place(any(), any(), any(), any(), any());
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void existingPaperPositionBlocksCandidateExecution() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(positionEntryGuardService.shouldBlockEntry(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any())).thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate entry blocked by existing paper position");
        verify(pendingLimitOrderService, never()).place(any(), any(), any(), any(), any());
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void pendingLimitOrderBlocksNewEntry() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(pendingLimitOrderService.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate entry blocked by pending limit order");
        verify(pendingLimitOrderService, never()).place(any(), any(), any(), any(), any());
    }

    @Test
    void sessionMarketCooldownBlocksRepeatedLimitRequestsInSameUtcSession() {
        when(candidateScannerService.scan(ExchangeMode.BINANCE, "BTCUSDT"))
                .thenReturn(binanceSelectedCandidate("2026-04-30T08:45:00Z"))
                .thenReturn(binanceSelectedCandidate("2026-04-30T08:50:00Z"));
        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any()))
                .thenReturn(true);

        TradingFlowResult first = service.execute(ExchangeMode.BINANCE, "BTCUSDT");
        TradingFlowResult second = service.execute(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(first.orderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(second.orderCreated()).isFalse();
        assertThat(second.message()).contains("session market cooldown");
        verify(pendingLimitOrderService, times(1))
                .tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any());
        verify(tradingFlowHistoryService).save(ExchangeMode.BINANCE, first);
        verify(tradingFlowHistoryService).save(ExchangeMode.BINANCE, second);
    }

    @Test
    void sessionMarketCooldownExpiresAfterUtcSessionEnd() {
        when(candidateScannerService.scan(ExchangeMode.BINANCE, "BTCUSDT"))
                .thenReturn(binanceSelectedCandidate("2026-04-30T08:45:00Z"))
                .thenReturn(binanceSelectedCandidate("2026-04-30T12:00:00Z"));
        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any()))
                .thenReturn(true);

        TradingFlowResult first = service.execute(ExchangeMode.BINANCE, "BTCUSDT");
        TradingFlowResult second = service.execute(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(first.orderStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(second.orderStatus()).isEqualTo(OrderStatus.REQUESTED);
        verify(pendingLimitOrderService, times(2))
                .tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any());
    }

    @Test
    void concurrentPendingLimitOrderPlacementBlocksNewEntry() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(), any(), any()))
                .thenReturn(false);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate entry blocked by pending limit order");
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void selectedCandidateUsesConfiguredOrderAmount() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        StrategyMarketOverrideProperties.MarketOverride override = new StrategyMarketOverrideProperties.MarketOverride();
        override.setOrderQuantity(new BigDecimal("0.02"));
        StrategyMarketOverrideProperties overrideProperties = new StrategyMarketOverrideProperties();
        overrideProperties.setMarkets(java.util.Map.of("KRW-BTC", override));
        service = new CandidateExecutionService(
                candidateScannerService,
                new StrategyMarketSettingsService(strategyProperties, new CandidateScannerProperties(), overrideProperties),
                tradingFlowHistoryService,
                candidateScanLogService,
                notificationProperties,
                notificationPolicyService,
                tradingFlowNotificationService,
                killSwitchService,
                positionEntryGuardService,
                pendingLimitOrderService
        );

        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(), any(), any()))
                .thenReturn(true);

        service.execute("KRW-BTC");

        ArgumentCaptor<BigDecimal> qtyCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(pendingLimitOrderService).tryPlace(
                eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(), qtyCaptor.capture(), any());
        assertThat(qtyCaptor.getValue()).isEqualByComparingTo("100.00000000");
    }

    @Test
    void killSwitchBlocksCandidateExecutionBeforeScan() {
        when(killSwitchService.isEnabled()).thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Kill switch enabled: candidate execution blocked");
        verify(candidateScannerService, never()).scan(ExchangeMode.UPBIT, "KRW-BTC");
        verify(pendingLimitOrderService, never()).place(any(), any(), any(), any(), any());
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void binanceCandidatePlacesLimitOrderForBinanceExchange() {
        when(candidateScannerService.scan(ExchangeMode.BINANCE, "BTCUSDT")).thenReturn(new TradingCandidate(
                "BTCUSDT",
                CandidateDecision.SELECTED,
                "Volatility long candidate selected",
                new BigDecimal("50000"),
                new BigDecimal("2.5"),
                new BigDecimal("5"),
                new BigDecimal("20"),
                MarketTrend.UP,
                true,
                Instant.parse("2026-04-30T00:00:00Z")
        ));
        when(pendingLimitOrderService.tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any()))
                .thenReturn(true);

        TradingFlowResult result = service.execute(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(result.market()).isEqualTo("BTCUSDT");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REQUESTED);
        verify(pendingLimitOrderService).tryPlace(eq(ExchangeMode.BINANCE), eq("BTCUSDT"), any(), any(), any());
        verify(tradingFlowHistoryService).save(ExchangeMode.BINANCE, result);
    }

    private TradingCandidate selectedCandidate() {
        return new TradingCandidate(
                "KRW-BTC",
                CandidateDecision.SELECTED,
                "Volatility long candidate selected",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("5"),
                new BigDecimal("20"),
                MarketTrend.UP,
                true,
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }

    private TradingCandidate skippedCandidate() {
        return new TradingCandidate(
                "KRW-BTC",
                CandidateDecision.SKIPPED,
                "Trend is not UP",
                new BigDecimal("100"),
                new BigDecimal("-1"),
                new BigDecimal("5"),
                new BigDecimal("20"),
                MarketTrend.DOWN,
                false,
                Instant.parse("2026-04-30T00:00:00Z")
        );
    }

    private TradingCandidate binanceSelectedCandidate(String scannedAt) {
        return new TradingCandidate(
                "BTCUSDT",
                CandidateDecision.SELECTED,
                "Session volatility breakout selected: Binance 15m UTC06-12 close-limit",
                new BigDecimal("50000"),
                new BigDecimal("2.5"),
                new BigDecimal("5"),
                new BigDecimal("20"),
                MarketTrend.UP,
                true,
                Instant.parse(scannedAt)
        );
    }

    private StrategyMarketSettingsService strategyMarketSettingsService() {
        return new StrategyMarketSettingsService(
                strategyProperties,
                new CandidateScannerProperties(),
                new StrategyMarketOverrideProperties()
        );
    }
}
