package com.giseop.comebot.analytics.dto;

import java.util.List;

public record AnalyticsLossResponse(
        String range,
        List<LossTradeResponse> worstTrades,
        List<MarketCountResponse> repeatedStopLossMarkets
) {
}
