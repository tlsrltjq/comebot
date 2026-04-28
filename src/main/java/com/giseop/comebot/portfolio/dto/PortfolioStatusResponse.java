package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioStatusResponse(
        BigDecimal cash,
        BigDecimal realizedProfit
) {
}
