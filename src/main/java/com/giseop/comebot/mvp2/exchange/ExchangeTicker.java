package com.giseop.comebot.mvp2.exchange;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeTicker(
        Exchange exchange,
        String symbol,
        BigDecimal tradePrice,
        Instant capturedAt
) {
}
