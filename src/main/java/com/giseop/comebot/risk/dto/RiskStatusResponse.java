package com.giseop.comebot.risk.dto;

import java.math.BigDecimal;
import java.util.List;

public record RiskStatusResponse(
        BigDecimal maxOrderAmount,
        List<String> allowedMarkets,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate,
        boolean positionExitEnabled,
        boolean dailyRiskEnabled,
        int dailyOrderLimit,
        BigDecimal dailyLossLimit,
        ConcentrationStatus concentration,
        StopLossCooldownStatus stopLossCooldown
) {
    public record ConcentrationStatus(
            String exchange,
            boolean enabled,
            BigDecimal warningExposureRate,
            BigDecimal blockExposureRate
    ) {
    }

    public record StopLossCooldownStatus(
            boolean enabled,
            String window,
            int triggerCount,
            String duration
    ) {
    }
}
