package com.giseop.comebot.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperRealizedProfit(
        BigDecimal profit,
        Instant realizedAt
) {
}
