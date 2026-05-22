package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.PositionEntryGuardService;
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
    private OrderExecutionService orderExecutionService;
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
                new OrderRequestFactory(),
                orderExecutionService,
                tradingFlowHistoryService,
                candidateScanLogService,
                notificationProperties,
                notificationPolicyService,
                tradingFlowNotificationService,
                killSwitchService,
                positionEntryGuardService
        );
    }

    @Test
    void selectedCandidateExecutesPaperBuyOrder() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(orderExecutionService.execute(eq(ExchangeMode.UPBIT), any(OrderRequest.class))).thenReturn(new OrderResult(
                "KRW-BTC",
                OrderSide.BUY,
                new BigDecimal("100.00000000"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-30T00:01:00Z")
        ));

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(com.giseop.comebot.strategy.domain.SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.currentPrice()).isEqualByComparingTo("100");

        ArgumentCaptor<OrderRequest> requestCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderExecutionService).execute(eq(ExchangeMode.UPBIT), requestCaptor.capture());
        assertThat(requestCaptor.getValue().market()).isEqualTo("KRW-BTC");
        assertThat(requestCaptor.getValue().side()).isEqualTo(OrderSide.BUY);
        assertThat(requestCaptor.getValue().quantity()).isEqualByComparingTo("100.00000000");
        assertThat(requestCaptor.getValue().price()).isEqualByComparingTo("100");
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void skippedCandidateDoesNotExecuteOrder() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(skippedCandidate());

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate was not selected");
        verify(orderExecutionService, never()).execute(any(OrderRequest.class));
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void existingPaperPositionBlocksCandidateExecution() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(positionEntryGuardService.shouldBlockEntry(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any())).thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.message()).isEqualTo("Candidate entry blocked by existing paper position");
        verify(orderExecutionService, never()).execute(any(OrderRequest.class));
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void selectedCandidateUsesConfiguredOrderAmount() {
        when(candidateScannerService.scan(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(selectedCandidate());
        when(orderExecutionService.execute(eq(ExchangeMode.UPBIT), any(OrderRequest.class))).thenReturn(new OrderResult(
                "KRW-BTC",
                OrderSide.BUY,
                new BigDecimal("100.00000000"),
                new BigDecimal("100"),
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-30T00:01:00Z")
        ));
        StrategyMarketOverrideProperties.MarketOverride override = new StrategyMarketOverrideProperties.MarketOverride();
        override.setOrderQuantity(new BigDecimal("0.02"));
        StrategyMarketOverrideProperties overrideProperties = new StrategyMarketOverrideProperties();
        overrideProperties.setMarkets(java.util.Map.of("KRW-BTC", override));
        service = new CandidateExecutionService(
                candidateScannerService,
                new StrategyMarketSettingsService(strategyProperties, new CandidateScannerProperties(), overrideProperties),
                new OrderRequestFactory(),
                orderExecutionService,
                tradingFlowHistoryService,
                candidateScanLogService,
                notificationProperties,
                notificationPolicyService,
                tradingFlowNotificationService,
                killSwitchService,
                positionEntryGuardService
        );

        service.execute("KRW-BTC");

        ArgumentCaptor<OrderRequest> requestCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderExecutionService).execute(eq(ExchangeMode.UPBIT), requestCaptor.capture());
        assertThat(requestCaptor.getValue().quantity()).isEqualByComparingTo("100.00000000");
    }

    @Test
    void killSwitchBlocksCandidateExecutionBeforeScan() {
        when(killSwitchService.isEnabled()).thenReturn(true);

        TradingFlowResult result = service.execute("KRW-BTC");

        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Kill switch enabled: candidate execution blocked");
        verify(candidateScannerService, never()).scan(ExchangeMode.UPBIT, "KRW-BTC");
        verify(orderExecutionService, never()).execute(any(OrderRequest.class));
        verify(tradingFlowHistoryService).save(ExchangeMode.UPBIT, result);
    }

    @Test
    void binanceCandidateUsesBinanceExecutionAndHistory() {
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
        when(orderExecutionService.execute(eq(ExchangeMode.BINANCE), any(OrderRequest.class))).thenReturn(new OrderResult(
                "BTCUSDT",
                OrderSide.BUY,
                new BigDecimal("0.20000000"),
                new BigDecimal("50000"),
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-30T00:01:00Z")
        ));

        TradingFlowResult result = service.execute(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(result.market()).isEqualTo("BTCUSDT");
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
        ArgumentCaptor<OrderRequest> requestCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderExecutionService).execute(eq(ExchangeMode.BINANCE), requestCaptor.capture());
        assertThat(requestCaptor.getValue().market()).isEqualTo("BTCUSDT");
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

    private StrategyMarketSettingsService strategyMarketSettingsService() {
        return new StrategyMarketSettingsService(
                strategyProperties,
                new CandidateScannerProperties(),
                new StrategyMarketOverrideProperties()
        );
    }
}
