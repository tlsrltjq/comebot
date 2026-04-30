package com.giseop.comebot.strategy.indicator;

import java.math.BigDecimal;

public record VolatilitySnapshot(
        String market,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        int candleCount
) {
}
