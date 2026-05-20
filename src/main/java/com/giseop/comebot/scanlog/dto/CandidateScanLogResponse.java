package com.giseop.comebot.scanlog.dto;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;

public record CandidateScanLogResponse(
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

    public static CandidateScanLogResponse from(CandidateScanLog log) {
        return new CandidateScanLogResponse(
                log.id(),
                log.exchange(),
                log.market(),
                log.decision(),
                log.reason(),
                log.currentPrice(),
                log.priceChangeRate(),
                log.highLowRangeRate(),
                log.tradeAmountChangeRate(),
                log.trend(),
                log.lastCandleBullish(),
                log.scannedAt()
        );
    }
}
