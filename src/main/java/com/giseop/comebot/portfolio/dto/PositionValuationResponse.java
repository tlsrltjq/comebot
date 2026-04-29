package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;

public record PositionValuationResponse(
        String market,
        BigDecimal quantity,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal positionValue,
        BigDecimal unrealizedProfit,
        BigDecimal unrealizedProfitRate
) {
}
