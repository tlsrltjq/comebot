package com.giseop.comebot.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceResponse(
        String market,
        BigDecimal currentPrice,
        Instant capturedAt
) {
}
