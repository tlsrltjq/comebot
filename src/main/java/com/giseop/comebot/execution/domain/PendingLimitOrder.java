package com.giseop.comebot.execution.domain;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.time.Instant;

public record PendingLimitOrder(
        ExchangeMode exchange,
        String market,
        BigDecimal limitPrice,
        BigDecimal quantity,
        String reason,
        Instant createdAt,
        Instant firstCheckAt,
        Instant expiresAt
) {
}
