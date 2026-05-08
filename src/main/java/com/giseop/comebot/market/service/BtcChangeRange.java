package com.giseop.comebot.market.service;

import java.util.Locale;

public enum BtcChangeRange {
    ONE_HOUR("1h", 1, 60),
    ONE_DAY("24h", 15, 96),
    THREE_DAYS("3d", 60, 72),
    SEVEN_DAYS("7d", 240, 42);

    private final String value;
    private final int candleUnitMinutes;
    private final int candleCount;

    BtcChangeRange(String value, int candleUnitMinutes, int candleCount) {
        this.value = value;
        this.candleUnitMinutes = candleUnitMinutes;
        this.candleCount = candleCount;
    }

    public String value() {
        return value;
    }

    public int candleUnitMinutes() {
        return candleUnitMinutes;
    }

    public int candleCount() {
        return candleCount;
    }

    public static BtcChangeRange from(String value) {
        String normalized = value == null || value.isBlank()
                ? ONE_DAY.value
                : value.trim().toLowerCase(Locale.ROOT);
        for (BtcChangeRange range : values()) {
            if (range.value.equals(normalized)) {
                return range;
            }
        }
        throw new IllegalArgumentException("Unsupported BTC change range: " + value);
    }
}
