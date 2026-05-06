package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.mvp2.exchange.Exchange;
import java.math.BigDecimal;
import java.util.List;

public record Mvp2PaperPortfolioValuation(
        Exchange exchange,
        BigDecimal cash,
        BigDecimal totalPositionValue,
        BigDecimal totalEquity,
        BigDecimal realizedProfit,
        BigDecimal unrealizedProfit,
        BigDecimal totalProfit,
        List<Mvp2PaperPositionValuation> positions
) {
}
