package com.giseop.comebot.portfolio.domain;

import java.math.BigDecimal;

public record PaperPosition(
        String market,
        BigDecimal quantity,
        BigDecimal averageBuyPrice
) {
}
