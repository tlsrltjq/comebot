package com.giseop.comebot.market.dto;

import java.math.BigDecimal;

public record MarketFlowEntry(
        String market,
        int rank,
        int prevRank,
        BigDecimal tradePrice,
        BigDecimal accTradePrice24h,
        double volumeSharePct,
        int selectedCount24h
) {
    public int rankChange() {
        return prevRank - rank;
    }
}
