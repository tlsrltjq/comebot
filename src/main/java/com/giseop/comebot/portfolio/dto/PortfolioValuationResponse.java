package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioValuationResponse(
        String exchange,
        String currency,
        BigDecimal cash,
        BigDecimal totalPositionValue,
        BigDecimal totalEquity,
        BigDecimal realizedProfit,
        BigDecimal unrealizedProfit,
        BigDecimal totalProfit,
        List<PositionValuationResponse> positions
) {
    public PortfolioValuationResponse(
            BigDecimal cash,
            BigDecimal totalPositionValue,
            BigDecimal totalEquity,
            BigDecimal realizedProfit,
            BigDecimal unrealizedProfit,
            BigDecimal totalProfit,
            List<PositionValuationResponse> positions
    ) {
        this("UPBIT", "KRW", cash, totalPositionValue, totalEquity, realizedProfit, unrealizedProfit, totalProfit, positions);
    }
}
