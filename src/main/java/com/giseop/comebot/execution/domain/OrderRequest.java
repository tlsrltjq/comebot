package com.giseop.comebot.execution.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderRequest(
        String market,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        Instant requestedAt
) {
}
