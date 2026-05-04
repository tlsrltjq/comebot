package com.giseop.comebot.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsPnlResponse(
        String range,
        Instant from,
        Instant to,
        BigDecimal cash,
        BigDecimal totalPositionValue,
        BigDecimal totalEquity,
        BigDecimal realizedProfit,
        BigDecimal unrealizedProfit,
        BigDecimal totalProfit,
        int positionCount
) {
}
