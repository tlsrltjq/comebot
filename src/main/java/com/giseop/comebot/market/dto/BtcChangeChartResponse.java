package com.giseop.comebot.market.dto;

import java.math.BigDecimal;
import java.util.List;

public record BtcChangeChartResponse(
        String exchange,
        String market,
        String range,
        BigDecimal basePrice,
        BigDecimal latestPrice,
        BigDecimal changeRate,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        List<BtcChangePointResponse> points
) {
}
