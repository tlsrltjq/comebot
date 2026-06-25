package com.giseop.comebot.market.candle.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record StockCandleRow(
        Instant timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {

    public StockCandleRow {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        validatePositive("open", open);
        validatePositive("high", high);
        validatePositive("low", low);
        validatePositive("close", close);
        if (volume == null || volume.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("volume must be zero or positive");
        }
        if (high.compareTo(low) < 0) {
            throw new IllegalArgumentException("high must be greater than or equal to low");
        }
        if (open.compareTo(high) > 0 || open.compareTo(low) < 0
                || close.compareTo(high) > 0 || close.compareTo(low) < 0) {
            throw new IllegalArgumentException("open and close must be within high-low range");
        }
    }

    private static void validatePositive(String name, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
