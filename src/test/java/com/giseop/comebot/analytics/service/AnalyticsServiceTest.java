package com.giseop.comebot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.analytics.dto.AnalyticsLossResponse;
import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.analytics.dto.AnalyticsSummaryResponse;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {

    private final Instant now = Instant.parse("2026-05-04T00:00:00Z");
    private final TradingFlowHistoryService historyService = mock(TradingFlowHistoryService.class);
    private final PaperPortfolioValuationService valuationService = mock(PaperPortfolioValuationService.class);
    private final AnalyticsService service = new AnalyticsService(
            historyService,
            valuationService,
            Clock.fixed(now, ZoneOffset.UTC)
    );

    @Test
    void summaryCountsSignalsAndExitReasons() {
        when(historyService.findSince(ExchangeMode.UPBIT, now.minus(AnalyticsRange.TWENTY_FOUR_HOURS.duration()))).thenReturn(List.of(
                history("KRW-BTC", SignalType.BUY, OrderStatus.FILLED, "Volatility long candidate selected", now.minusSeconds(7200)),
                history("KRW-BTC", SignalType.SELL, OrderStatus.FILLED, "Stop loss rate reached: -1.25000000", now.minusSeconds(3600)),
                history("KRW-ETH", SignalType.BUY, OrderStatus.FILLED, "Volatility long candidate selected", now.minusSeconds(1800)),
                history("KRW-ETH", SignalType.SELL, OrderStatus.FILLED, "Take profit rate reached: 2.50000000", now.minusSeconds(600)),
                history("KRW-XRP", SignalType.HOLD, null, "Trend is not UP"),
                history("KRW-SOL", SignalType.HOLD, null, "Trend is not UP")
        ));

        AnalyticsSummaryResponse response = service.summary(AnalyticsRange.TWENTY_FOUR_HOURS);

        assertThat(response.total()).isEqualTo(6);
        assertThat(response.buyCount()).isEqualTo(2);
        assertThat(response.sellCount()).isEqualTo(2);
        assertThat(response.holdCount()).isEqualTo(2);
        assertThat(response.filledCount()).isEqualTo(4);
        assertThat(response.stopLossCount()).isEqualTo(1);
        assertThat(response.takeProfitCount()).isEqualTo(1);
        assertThat(response.averageStopLossRate()).isEqualByComparingTo("-1.25000000");
        assertThat(response.averageTakeProfitRate()).isEqualByComparingTo("2.50000000");
        assertThat(response.winRate()).isEqualByComparingTo("50.00000000");
        assertThat(response.averageHoldingSeconds()).isEqualTo(2400);
        assertThat(response.profitLossRatio()).isEqualByComparingTo("2.00000000");
        assertThat(response.topHoldReasons().getFirst().reason()).isEqualTo("Trend is not UP");
        assertThat(response.topHoldReasons().getFirst().count()).isEqualTo(2);
    }

    @Test
    void pnlUsesPortfolioValuation() {
        when(valuationService.valuate(ExchangeMode.UPBIT)).thenReturn(new PortfolioValuationResponse(
                new BigDecimal("900000"),
                new BigDecimal("120000"),
                new BigDecimal("1020000"),
                new BigDecimal("10000"),
                new BigDecimal("10000"),
                new BigDecimal("20000"),
                List.of()
        ));

        assertThat(service.pnl(AnalyticsRange.ONE_HOUR).totalProfit()).isEqualByComparingTo("20000");
    }

    @Test
    void summaryUsesRequestedExchange() {
        when(historyService.findSince(ExchangeMode.BINANCE, now.minus(AnalyticsRange.ONE_HOUR.duration()))).thenReturn(List.of(
                history(ExchangeMode.BINANCE, "BTCUSDT", SignalType.BUY, OrderStatus.FILLED, "Volatility long candidate selected")
        ));

        AnalyticsSummaryResponse response = service.summary(AnalyticsRange.ONE_HOUR, ExchangeMode.BINANCE);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.buyCount()).isEqualTo(1);
    }

    @Test
    void lossesReturnWorstStopLossTrades() {
        when(historyService.findSince(ExchangeMode.UPBIT, now.minus(AnalyticsRange.ONE_HOUR.duration()))).thenReturn(List.of(
                history("KRW-BTC", SignalType.SELL, OrderStatus.FILLED, "Stop loss rate reached: -0.80000000"),
                history("KRW-ETH", SignalType.SELL, OrderStatus.FILLED, "Stop loss rate reached: -2.00000000"),
                history("KRW-XRP", SignalType.SELL, OrderStatus.FILLED, "Take profit rate reached: 1.80000000")
        ));

        AnalyticsLossResponse response = service.losses(AnalyticsRange.ONE_HOUR);

        assertThat(response.worstTrades()).hasSize(2);
        assertThat(response.worstTrades().getFirst().market()).isEqualTo("KRW-ETH");
        assertThat(response.worstTrades().getFirst().rate()).isEqualByComparingTo("-2.00000000");
    }

    private TradingFlowHistory history(String market, SignalType signalType, OrderStatus orderStatus, String reason) {
        return history(ExchangeMode.UPBIT, market, signalType, orderStatus, reason, now);
    }

    private TradingFlowHistory history(ExchangeMode exchange, String market, SignalType signalType, OrderStatus orderStatus, String reason) {
        return history(exchange, market, signalType, orderStatus, reason, now);
    }

    private TradingFlowHistory history(String market, SignalType signalType, OrderStatus orderStatus, String reason, Instant createdAt) {
        return history(ExchangeMode.UPBIT, market, signalType, orderStatus, reason, createdAt);
    }

    private TradingFlowHistory history(ExchangeMode exchange, String market, SignalType signalType, OrderStatus orderStatus, String reason, Instant createdAt) {
        return new TradingFlowHistory(
                java.util.UUID.randomUUID().toString(),
                exchange,
                market,
                new BigDecimal("100"),
                signalType,
                reason,
                orderStatus != null,
                orderStatus,
                "message",
                createdAt
        );
    }
}
