package com.giseop.comebot.scanlog.domain;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record CandidateScanLog(
        String id,
        ExchangeMode exchange,
        String market,
        CandidateDecision decision,
        String reason,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        BigDecimal highLowRangeRate,
        BigDecimal tradeAmountChangeRate,
        MarketTrend trend,
        Boolean lastCandleBullish,
        Instant scannedAt
) {
}
