package com.giseop.comebot.strategy.service;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class SimpleThresholdStrategy implements TradingStrategy {

    private static final int QUANTITY_SCALE = 8;

    private final StrategyProperties strategyProperties;

    public SimpleThresholdStrategy(StrategyProperties strategyProperties) {
        this.strategyProperties = strategyProperties;
    }

    @Override
    public TradingSignal evaluate(MarketPrice marketPrice) {
        if (marketPrice == null || marketPrice.currentPrice() == null) {
            return hold(null, null, "Market price is not available");
        }
        if (marketPrice.currentPrice().compareTo(strategyProperties.getBuyPrice()) <= 0) {
            return new TradingSignal(
                    marketPrice.market(),
                    SignalType.BUY,
                    "Test threshold buy signal",
                    marketPrice.currentPrice(),
                    buyQuantity(marketPrice.currentPrice()),
                    Instant.now()
            );
        }
        if (marketPrice.currentPrice().compareTo(strategyProperties.getSellPrice()) >= 0) {
            return new TradingSignal(
                    marketPrice.market(),
                    SignalType.SELL,
                    "Test threshold sell signal",
                    marketPrice.currentPrice(),
                    strategyProperties.getOrderQuantity(),
                    Instant.now()
            );
        }
        return hold(marketPrice.market(), marketPrice.currentPrice(), "No test threshold matched");
    }

    private TradingSignal hold(String market, BigDecimal price, String reason) {
        return new TradingSignal(
                market,
                SignalType.HOLD,
                reason,
                price,
                BigDecimal.ZERO,
                Instant.now()
        );
    }

    private BigDecimal buyQuantity(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return strategyProperties.getOrderQuantity();
        }
        return strategyProperties.getOrderAmount().divide(price, QUANTITY_SCALE, RoundingMode.DOWN);
    }
}
