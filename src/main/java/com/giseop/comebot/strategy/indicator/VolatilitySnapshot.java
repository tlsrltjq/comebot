package com.giseop.comebot.strategy.indicator;

import java.math.BigDecimal;

public record VolatilitySnapshot(
        String market,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        int candleCount,
        boolean lastCandleBullish,
        BigDecimal latestCandleTradeAmount,
        BigDecimal distanceFromHighRate,
        BigDecimal windowHighChangeRate,
        BigDecimal peakTradeAmountChangeRate,
        BigDecimal volumeCooldownRatio,
        int consecutiveBullishCandles,
        BigDecimal priceRecoveryRate
) {
}
