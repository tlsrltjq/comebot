package com.giseop.comebot.risk.domain;

import java.math.BigDecimal;

public record PositionExitPolicy(
        boolean enabled,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate
) {
}
