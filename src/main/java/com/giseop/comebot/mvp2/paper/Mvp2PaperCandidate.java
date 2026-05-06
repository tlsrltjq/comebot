package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record Mvp2PaperCandidate(
        Exchange exchange,
        String symbol,
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
