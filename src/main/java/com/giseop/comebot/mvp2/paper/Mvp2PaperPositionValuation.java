package com.giseop.comebot.mvp2.paper;

import java.math.BigDecimal;

public record Mvp2PaperPositionValuation(
        String symbol,
        BigDecimal quantity,
        BigDecimal averageBuyPrice,
        BigDecimal currentPrice,
        BigDecimal positionValue,
        BigDecimal unrealizedProfit,
        BigDecimal unrealizedProfitRate
) {
}
