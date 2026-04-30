package com.giseop.comebot.market.candle.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Candle(
        String market,
        Instant candleTime,
        BigDecimal openingPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal tradePrice,
        BigDecimal accumulatedTradePrice,
        BigDecimal accumulatedTradeVolume
) {
}
