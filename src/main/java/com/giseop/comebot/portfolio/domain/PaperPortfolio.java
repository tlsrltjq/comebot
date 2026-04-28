package com.giseop.comebot.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

public record PaperPortfolio(
        BigDecimal cash,
        BigDecimal realizedProfit,
        List<PaperPosition> positions
) {
}
