package com.giseop.comebot.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AnalyticsSummaryResponse(
        String range,
        Instant from,
        Instant to,
        long total,
        long buyCount,
        long sellCount,
        long holdCount,
        long filledCount,
        long rejectedCount,
        long failedCount,
        long stopLossCount,
        long takeProfitCount,
        BigDecimal averageStopLossRate,
        BigDecimal averageTakeProfitRate,
        List<ReasonCountResponse> topHoldReasons,
        List<MarketCountResponse> topMarkets
) {
}
