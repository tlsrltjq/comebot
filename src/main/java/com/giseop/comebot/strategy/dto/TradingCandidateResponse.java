package com.giseop.comebot.strategy.dto;

import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingCandidateResponse(
        String market,
        CandidateDecision decision,
        String reason,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        Instant scannedAt
) {

    public static TradingCandidateResponse from(TradingCandidate candidate) {
        return new TradingCandidateResponse(
                candidate.market(),
                candidate.decision(),
                candidate.reason(),
                candidate.priceChangeRate(),
                candidate.highLowRangeRate(),
                candidate.tradeAmountChangeRate(),
                candidate.trend(),
                candidate.scannedAt()
        );
    }
}
