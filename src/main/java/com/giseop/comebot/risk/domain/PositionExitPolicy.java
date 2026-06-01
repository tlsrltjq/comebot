package com.giseop.comebot.risk.domain;

import java.math.BigDecimal;

public record PositionExitPolicy(
        boolean enabled,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate,
        boolean trailingStopEnabled,
        BigDecimal trailingStopActivationRate,
        BigDecimal trailingStopTrailRate,
        BigDecimal abnormalExitPriceDropRate
) {
}
