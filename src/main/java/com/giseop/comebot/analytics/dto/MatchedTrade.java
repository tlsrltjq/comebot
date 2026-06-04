package com.giseop.comebot.analytics.dto;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.time.Instant;

public record MatchedTrade(
        ExchangeMode exchange,
        String market,
        Instant buyAt,
        BigDecimal buyPrice,
        Instant sellAt,
        BigDecimal sellPrice,
        long holdingSeconds,
        BigDecimal profitRatePct,
        String exitReason
) {
}
