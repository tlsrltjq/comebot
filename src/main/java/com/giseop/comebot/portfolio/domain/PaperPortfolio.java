package com.giseop.comebot.portfolio.domain;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.util.List;

public record PaperPortfolio(
        ExchangeMode exchange,
        String currency,
        BigDecimal cash,
        BigDecimal realizedProfit,
        List<PaperPosition> positions
) {
    public PaperPortfolio(BigDecimal cash, BigDecimal realizedProfit, List<PaperPosition> positions) {
        this(ExchangeMode.UPBIT, "KRW", cash, realizedProfit, positions);
    }

    public static String currencyFor(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? "USDT" : "KRW";
    }
}
