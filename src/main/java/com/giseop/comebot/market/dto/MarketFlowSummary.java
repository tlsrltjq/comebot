package com.giseop.comebot.market.dto;

import com.giseop.comebot.exchange.ExchangeMode;
import java.util.List;

public record MarketFlowSummary(
        ExchangeMode exchange,
        double btcDominancePct,
        double top10VolumePct,
        List<MarketFlowEntry> markets
) {
}
