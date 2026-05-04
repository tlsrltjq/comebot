package com.giseop.comebot.strategy.dto;

import java.math.BigDecimal;

public record StrategyStatusResponse(
        String strategyName,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        BigDecimal orderQuantity,
        BigDecimal orderAmount
) {
}
