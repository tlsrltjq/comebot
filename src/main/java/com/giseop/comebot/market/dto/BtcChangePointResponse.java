package com.giseop.comebot.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BtcChangePointResponse(
        Instant time,
        BigDecimal price,
        BigDecimal changeRate
) {
}
