package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.mvp2.exchange.Exchange;
import java.math.BigDecimal;
import java.util.List;

public record Mvp2PaperPortfolioSnapshot(
        Exchange exchange,
        BigDecimal cash,
        BigDecimal realizedProfit,
        List<Mvp2PaperPosition> positions
) {
}
