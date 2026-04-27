package com.giseop.comebot.market.dto;

import java.math.BigDecimal;

public record MarketPriceUpdateRequest(
        String market,
        BigDecimal price
) {
}
