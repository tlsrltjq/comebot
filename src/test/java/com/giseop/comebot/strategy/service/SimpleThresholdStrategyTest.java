package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleThresholdStrategyTest {

    private SimpleThresholdStrategy strategy;

    @BeforeEach
    void setUp() {
        StrategyProperties properties = new StrategyProperties();
        properties.setBuyPrice(new BigDecimal("100"));
        properties.setSellPrice(new BigDecimal("200"));
        properties.setOrderQuantity(new BigDecimal("0.5"));
        strategy = new SimpleThresholdStrategy(properties);
    }

    @Test
    void evaluateReturnsBuySignalWhenPriceIsAtOrBelowBuyThreshold() {
        TradingSignal signal = strategy.evaluate(marketPrice("100"));

        assertThat(signal.signalType()).isEqualTo(SignalType.BUY);
        assertThat(signal.market()).isEqualTo("KRW-BTC");
        assertThat(signal.targetPrice()).isEqualByComparingTo("100");
        assertThat(signal.quantity()).isEqualByComparingTo("0.5");
    }

    @Test
    void evaluateReturnsSellSignalWhenPriceIsAtOrAboveSellThreshold() {
        TradingSignal signal = strategy.evaluate(marketPrice("200"));

        assertThat(signal.signalType()).isEqualTo(SignalType.SELL);
        assertThat(signal.market()).isEqualTo("KRW-BTC");
        assertThat(signal.targetPrice()).isEqualByComparingTo("200");
        assertThat(signal.quantity()).isEqualByComparingTo("0.5");
    }

    @Test
    void evaluateReturnsHoldSignalWhenNoThresholdMatches() {
        TradingSignal signal = strategy.evaluate(marketPrice("150"));

        assertThat(signal.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.market()).isEqualTo("KRW-BTC");
        assertThat(signal.targetPrice()).isEqualByComparingTo("150");
    }

    private MarketPrice marketPrice(String price) {
        return new MarketPrice("KRW-BTC", new BigDecimal(price), Instant.now());
    }
}
