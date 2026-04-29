package com.giseop.comebot.risk.dto;

import java.math.BigDecimal;
import java.util.List;

public record RiskStatusResponse(
        BigDecimal maxOrderAmount,
        List<String> allowedMarkets,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate,
        boolean positionExitEnabled
) {
}
