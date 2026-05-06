package com.giseop.comebot.mvp2.paper.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingProperties;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingResult;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class Mvp2BinancePaperTradingSchedulerTest {

    @Test
    void runOnceDoesNotExecuteWhenDisabled() {
        Mvp2PaperTradingProperties properties = properties(false, List.of("BTCUSDT"));
        Mvp2PaperTradingService tradingService = mock(Mvp2PaperTradingService.class);

        Mvp2PaperSchedulerRunSummary summary = scheduler(properties, tradingService).runOnce();

        verify(tradingService, never()).run(Exchange.BINANCE, "BTCUSDT");
        assertThat(summary.enabled()).isFalse();
        assertThat(summary.requestedSymbols()).isZero();
    }

    @Test
    void runOnceExecutesConfiguredSymbolsWhenEnabled() {
        Mvp2PaperTradingProperties properties = properties(true, List.of("BTCUSDT", "ETHUSDT"));
        Mvp2PaperTradingService tradingService = mock(Mvp2PaperTradingService.class);
        when(tradingService.run(Exchange.BINANCE, "BTCUSDT")).thenReturn(result("BTCUSDT", OrderSide.BUY));
        when(tradingService.run(Exchange.BINANCE, "ETHUSDT")).thenReturn(result("ETHUSDT", null));

        Mvp2PaperSchedulerRunSummary summary = scheduler(properties, tradingService).runOnce();

        verify(tradingService).run(Exchange.BINANCE, "BTCUSDT");
        verify(tradingService).run(Exchange.BINANCE, "ETHUSDT");
        assertThat(summary.enabled()).isTrue();
        assertThat(summary.requestedSymbols()).isEqualTo(2);
        assertThat(summary.executedSymbols()).isEqualTo(2);
        assertThat(summary.buyCount()).isEqualTo(1);
        assertThat(summary.holdCount()).isEqualTo(1);
    }

    @Test
    void runOnceSkipsBlankSymbols() {
        Mvp2PaperTradingProperties properties = properties(true, List.of("BTCUSDT", " "));
        Mvp2PaperTradingService tradingService = mock(Mvp2PaperTradingService.class);
        when(tradingService.run(Exchange.BINANCE, "BTCUSDT")).thenReturn(result("BTCUSDT", OrderSide.SELL));

        Mvp2PaperSchedulerRunSummary summary = scheduler(properties, tradingService).runOnce();

        verify(tradingService).run(Exchange.BINANCE, "BTCUSDT");
        verify(tradingService, never()).run(Exchange.BINANCE, " ");
        assertThat(summary.requestedSymbols()).isEqualTo(1);
        assertThat(summary.sellCount()).isEqualTo(1);
    }

    @Test
    void symbolFailureDoesNotStopNextSymbol() {
        Mvp2PaperTradingProperties properties = properties(true, List.of("BTCUSDT", "ETHUSDT"));
        Mvp2PaperTradingService tradingService = mock(Mvp2PaperTradingService.class);
        when(tradingService.run(Exchange.BINANCE, "BTCUSDT")).thenThrow(new IllegalStateException("failed"));
        when(tradingService.run(Exchange.BINANCE, "ETHUSDT")).thenReturn(result("ETHUSDT", null));

        Mvp2PaperSchedulerRunSummary summary = scheduler(properties, tradingService).runOnce();

        verify(tradingService).run(Exchange.BINANCE, "BTCUSDT");
        verify(tradingService).run(Exchange.BINANCE, "ETHUSDT");
        assertThat(summary.requestedSymbols()).isEqualTo(2);
        assertThat(summary.executedSymbols()).isEqualTo(1);
        assertThat(summary.failedCount()).isEqualTo(1);
        assertThat(summary.holdCount()).isEqualTo(1);
    }

    private Mvp2BinancePaperTradingScheduler scheduler(
            Mvp2PaperTradingProperties properties,
            Mvp2PaperTradingService tradingService
    ) {
        return new Mvp2BinancePaperTradingScheduler(properties, tradingService);
    }

    private Mvp2PaperTradingProperties properties(boolean enabled, List<String> symbols) {
        Mvp2PaperTradingProperties properties = new Mvp2PaperTradingProperties();
        properties.setBinanceSchedulerEnabled(enabled);
        properties.setBinanceSymbols(symbols);
        return properties;
    }

    private Mvp2PaperTradingResult result(String symbol, OrderSide side) {
        return new Mvp2PaperTradingResult(
                Exchange.BINANCE,
                symbol,
                side,
                new BigDecimal("0.1"),
                new BigDecimal("100"),
                side == null ? null : OrderStatus.FILLED,
                "test",
                "test",
                Instant.now()
        );
    }
}
