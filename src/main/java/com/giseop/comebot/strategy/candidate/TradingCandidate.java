package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingCandidate(
        String market,
        CandidateDecision decision,
        String reason,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        Instant scannedAt
) {
}
