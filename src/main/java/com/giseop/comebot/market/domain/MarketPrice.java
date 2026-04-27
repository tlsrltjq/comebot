package com.giseop.comebot.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPrice(
        String market,
        BigDecimal currentPrice,
        Instant capturedAt
) {
}
