package com.giseop.comebot.analytics.dto;

import java.time.Duration;

public enum AnalyticsRange {
    ONE_HOUR("1h", Duration.ofHours(1)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24)),
    THREE_DAYS("3d", Duration.ofDays(3)),
    SEVEN_DAYS("7d", Duration.ofDays(7));

    private final String value;
    private final Duration duration;

    AnalyticsRange(String value, Duration duration) {
        this.value = value;
        this.duration = duration;
    }

    public String value() {
        return value;
    }

    public Duration duration() {
        return duration;
    }

    public static AnalyticsRange from(String value) {
        if (value == null || value.isBlank()) {
            return TWENTY_FOUR_HOURS;
        }
        for (AnalyticsRange range : values()) {
            if (range.value.equalsIgnoreCase(value.trim())) {
                return range;
            }
        }
        throw new IllegalArgumentException("Unsupported analytics range: " + value);
    }
}
