package com.giseop.comebot.portfolio.dto;

import java.math.BigDecimal;

public record PositionResponse(
        String market,
        BigDecimal quantity,
        BigDecimal averageBuyPrice
) {
}
