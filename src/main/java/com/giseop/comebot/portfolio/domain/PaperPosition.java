package com.giseop.comebot.portfolio.domain;

import java.math.BigDecimal;

public record PaperPosition(
        String market,
        BigDecimal quantity,
        BigDecimal averageBuyPrice,
        BigDecimal peakPrice
) {
    public PaperPosition(String market, BigDecimal quantity, BigDecimal averageBuyPrice) {
        this(market, quantity, averageBuyPrice, null);
    }
}
