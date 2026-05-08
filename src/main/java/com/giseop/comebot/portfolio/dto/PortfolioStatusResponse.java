package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioStatusResponse(
        String exchange,
        String currency,
        BigDecimal cash,
        BigDecimal realizedProfit
) {
    public PortfolioStatusResponse(BigDecimal cash, BigDecimal realizedProfit) {
        this("UPBIT", "KRW", cash, realizedProfit);
    }
}
