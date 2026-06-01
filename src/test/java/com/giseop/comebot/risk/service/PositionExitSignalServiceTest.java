package com.giseop.comebot.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PositionExitSignalServiceTest {

    @Test
    void evaluateReturnsEmptyWhenPolicyIsDisabled() {
        PositionExitProperties properties = new PositionExitProperties();
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "105"));

        assertThat(signal).isEmpty();
    }

    @Test
    void evaluateReturnsSellWhenTakeProfitRateIsReached() {
        PositionExitProperties properties = enabledProperties();
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "105"));

        assertThat(signal).isPresent();
        assertThat(signal.get().signalType()).isEqualTo(SignalType.SELL);
        assertThat(signal.get().reason()).contains("Take profit rate reached");
    }

    @Test
    void evaluateReturnsSellWhenStopLossRateIsReached() {
        PositionExitProperties properties = enabledProperties();
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "97"));

        assertThat(signal).isPresent();
        assertThat(signal.get().signalType()).isEqualTo(SignalType.SELL);
        assertThat(signal.get().reason()).contains("Stop loss rate reached");
    }

    @Test
    void evaluateReturnsEmptyWhenExitPriceDropLooksAbnormal() {
        PositionExitProperties properties = enabledProperties();
        properties.setAbnormalExitPriceDropRate(new BigDecimal("-20"));
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "10"));

        assertThat(signal).isEmpty();
    }

    @Test
    void evaluateReturnsEmptyWhenPositionDoesNotExist() {
        PositionExitProperties properties = enabledProperties();
        PaperPortfolioService portfolioService = portfolioService(List.of());

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "105"));

        assertThat(signal).isEmpty();
    }

    @Test
    void sellQuantityDoesNotExceedHeldQuantity() {
        PositionExitProperties properties = enabledProperties();
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.25"), new BigDecimal("100"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "105"));

        assertThat(signal).isPresent();
        assertThat(signal.get().quantity()).isEqualByComparingTo("0.25");
    }

    @Test
    void trailingStopTriggersWhenDrawdownExceedsTrailRate() {
        PositionExitProperties properties = enabledProperties();
        properties.setTrailingStopEnabled(true);
        properties.setTrailingStopActivationRate(new BigDecimal("0.5"));
        properties.setTrailingStopTrailRate(new BigDecimal("0.3"));
        // peakPrice=102 → peakProfit=+2% ≥ activation(0.5%), drawdown=(102-101.6)/102=0.39% ≥ trail(0.3%) → SELL
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"), new BigDecimal("102"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "101.6"));

        assertThat(signal).isPresent();
        assertThat(signal.get().signalType()).isEqualTo(SignalType.SELL);
        assertThat(signal.get().reason()).contains("Trailing stop triggered");
    }

    @Test
    void trailingStopDoesNotTriggerWhenDrawdownBelowTrailRate() {
        PositionExitProperties properties = enabledProperties();
        properties.setTrailingStopEnabled(true);
        properties.setTrailingStopActivationRate(new BigDecimal("0.5"));
        properties.setTrailingStopTrailRate(new BigDecimal("0.3"));
        // peakPrice=102, currentPrice=101.8 → drawdown=(102-101.8)/102=0.196% < trail(0.3%) → HOLD
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"), new BigDecimal("102"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "101.8"));

        assertThat(signal).isEmpty();
    }

    @Test
    void trailingStopDoesNotTriggerWhenPeakProfitBelowActivationRate() {
        PositionExitProperties properties = enabledProperties();
        properties.setTrailingStopEnabled(true);
        properties.setTrailingStopActivationRate(new BigDecimal("0.5"));
        properties.setTrailingStopTrailRate(new BigDecimal("0.3"));
        // peakPrice=100.3 → peakProfit=+0.3% < activation(0.5%) → not activated → HOLD
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"), new BigDecimal("100.3"))
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "100"));

        assertThat(signal).isEmpty();
    }

    @Test
    void trailingStopDoesNotTriggerWhenNoPeakPrice() {
        PositionExitProperties properties = enabledProperties();
        properties.setTrailingStopEnabled(true);
        properties.setTrailingStopActivationRate(new BigDecimal("0.5"));
        properties.setTrailingStopTrailRate(new BigDecimal("0.3"));
        // peakPrice=null → trailing not active yet → HOLD
        PaperPortfolioService portfolioService = portfolioService(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("1"), new BigDecimal("100"), null)
        ));

        Optional<TradingSignal> signal = new PositionExitSignalService(properties, portfolioService)
                .evaluate(price("KRW-BTC", "99.5"));

        assertThat(signal).isEmpty();
    }

    private PositionExitProperties enabledProperties() {
        PositionExitProperties properties = new PositionExitProperties();
        properties.setPositionExitEnabled(true);
        properties.setTakeProfitRate(new BigDecimal("5"));
        properties.setStopLossRate(new BigDecimal("-3"));
        return properties;
    }

    private PaperPortfolioService portfolioService(List<PaperPosition> positions) {
        PaperPortfolioService service = mock(PaperPortfolioService.class);
        when(service.findPositions()).thenReturn(positions);
        when(service.findPositions(com.giseop.comebot.exchange.ExchangeMode.UPBIT)).thenReturn(positions);
        return service;
    }

    private MarketPrice price(String market, String currentPrice) {
        return new MarketPrice(market, new BigDecimal(currentPrice), Instant.now());
    }
}
