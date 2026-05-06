package com.giseop.comebot.mvp2.paper.dto;

import java.math.BigDecimal;
import java.util.List;

public record Mvp2PaperStatusResponse(
        boolean schedulerEnabled,
        long schedulerFixedDelayMs,
        List<String> symbols,
        BigDecimal initialCash,
        BigDecimal orderAmount,
        BigDecimal takeProfitRate,
        BigDecimal stopLossRate
) {
}
