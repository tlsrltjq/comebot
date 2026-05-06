package com.giseop.comebot.mvp2.exchange;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeCandle(
        Exchange exchange,
        String symbol,
        Instant candleTime,
        BigDecimal openingPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal tradePrice,
        BigDecimal accumulatedTradePrice,
        BigDecimal accumulatedTradeVolume
) {
}
