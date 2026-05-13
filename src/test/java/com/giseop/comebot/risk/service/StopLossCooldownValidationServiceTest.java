package com.giseop.comebot.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.risk.StopLossCooldownProperties;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class StopLossCooldownValidationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-13T00:00:00Z"), ZoneOffset.UTC);
    private static final Instant WINDOW_START = Instant.parse("2026-05-06T00:00:00Z");

    @Test
    void disabledCooldownApprovesBuy() {
        StopLossCooldownProperties properties = properties(false);

        var result = service(properties, List.of(stopLoss("1", "KRW-BTC", "2026-05-12T00:00:00Z")))
                .validate(ExchangeMode.UPBIT, buy("KRW-BTC"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void rejectsBuyWhenStopLossCountReachesTriggerInsideCooldownDuration() {
        StopLossCooldownProperties properties = properties(true);

        var result = service(properties, List.of(
                stopLoss("1", "KRW-BTC", "2026-05-12T00:00:00Z"),
                stopLoss("2", "KRW-BTC", "2026-05-12T12:00:00Z")
        )).validate(ExchangeMode.UPBIT, buy("KRW-BTC"));

        assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
        assertThat(result.reason()).contains("Stop loss cooldown active", "KRW-BTC", "stopLossCount=2");
    }

    @Test
    void approvesBuyWhenLatestStopLossCooldownExpired() {
        StopLossCooldownProperties properties = properties(true);

        var result = service(properties, List.of(
                stopLoss("1", "KRW-BTC", "2026-05-10T00:00:00Z"),
                stopLoss("2", "KRW-BTC", "2026-05-11T00:00:00Z")
        )).validate(ExchangeMode.UPBIT, buy("KRW-BTC"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void ignoresRejectedFailedTakeProfitAndOtherMarkets() {
        StopLossCooldownProperties properties = properties(true);

        var result = service(properties, List.of(
                history("1", "KRW-BTC", SignalType.SELL, OrderStatus.REJECTED, "Stop loss rate reached: -0.8", "2026-05-12T12:00:00Z"),
                history("2", "KRW-BTC", SignalType.SELL, OrderStatus.FAILED, "Stop loss rate reached: -0.9", "2026-05-12T13:00:00Z"),
                history("3", "KRW-BTC", SignalType.SELL, OrderStatus.FILLED, "Take profit rate reached: 1.5", "2026-05-12T14:00:00Z"),
                stopLoss("4", "KRW-ETH", "2026-05-12T15:00:00Z")
        )).validate(ExchangeMode.UPBIT, buy("KRW-BTC"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void sellOrdersAreNotBlockedByCooldown() {
        StopLossCooldownProperties properties = properties(true);

        var result = service(properties, List.of(
                stopLoss("1", "KRW-BTC", "2026-05-12T00:00:00Z"),
                stopLoss("2", "KRW-BTC", "2026-05-12T12:00:00Z")
        )).validate(ExchangeMode.UPBIT, new OrderRequest(
                "KRW-BTC",
                OrderSide.SELL,
                BigDecimal.ONE,
                new BigDecimal("10000"),
                Instant.now(CLOCK)
        ));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    private StopLossCooldownValidationService service(
            StopLossCooldownProperties properties,
            List<TradingFlowHistory> histories
    ) {
        TradingFlowHistoryService historyService = mock(TradingFlowHistoryService.class);
        when(historyService.findSince(ExchangeMode.UPBIT, WINDOW_START)).thenReturn(histories);
        return new StopLossCooldownValidationService(properties, historyService, CLOCK);
    }

    private StopLossCooldownProperties properties(boolean enabled) {
        StopLossCooldownProperties properties = new StopLossCooldownProperties();
        properties.setEnabled(enabled);
        properties.setWindow(Duration.ofDays(7));
        properties.setTriggerCount(2);
        properties.setDuration(Duration.ofHours(24));
        return properties;
    }

    private TradingFlowHistory stopLoss(String id, String market, String createdAt) {
        return history(id, market, SignalType.SELL, OrderStatus.FILLED, "Stop loss rate reached: -0.8", createdAt);
    }

    private TradingFlowHistory history(
            String id,
            String market,
            SignalType signalType,
            OrderStatus orderStatus,
            String reason,
            String createdAt
    ) {
        return new TradingFlowHistory(
                id,
                ExchangeMode.UPBIT,
                market,
                new BigDecimal("100"),
                signalType,
                reason,
                true,
                orderStatus,
                "test",
                Instant.parse(createdAt)
        );
    }

    private OrderRequest buy(String market) {
        return new OrderRequest(market, OrderSide.BUY, BigDecimal.ONE, new BigDecimal("10000"), Instant.now(CLOCK));
    }
}
