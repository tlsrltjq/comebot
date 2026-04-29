package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioValuationResponse(
        BigDecimal cash,
        BigDecimal totalPositionValue,
        BigDecimal totalEquity,
        BigDecimal realizedProfit,
        BigDecimal unrealizedProfit,
        BigDecimal totalProfit,
        List<PositionValuationResponse> positions
) {
}
