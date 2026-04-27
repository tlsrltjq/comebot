package com.giseop.comebot.strategy.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TradingSignal(
        String market,
        SignalType signalType,
        String reason,
        BigDecimal targetPrice,
        BigDecimal quantity,
        Instant detectedAt
) {
}
