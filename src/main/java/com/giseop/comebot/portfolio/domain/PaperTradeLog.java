package com.giseop.comebot.portfolio.domain;

import com.giseop.comebot.execution.domain.OrderSide;
import java.math.BigDecimal;
import java.time.Instant;

public record PaperTradeLog(
        String market,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal grossAmount,
        BigDecimal realizedProfit,
        Instant executedAt
) {
}
