package com.giseop.comebot.mvp2.paper;

import java.math.BigDecimal;

public record Mvp2PaperPosition(
        String symbol,
        BigDecimal quantity,
        BigDecimal averageBuyPrice
) {
}
