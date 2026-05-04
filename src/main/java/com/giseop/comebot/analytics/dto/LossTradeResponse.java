package com.giseop.comebot.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LossTradeResponse(
        String market,
        BigDecimal currentPrice,
        BigDecimal rate,
        String reason,
        Instant createdAt
) {
}
