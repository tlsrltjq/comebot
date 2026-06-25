package com.giseop.comebot.market.candle.domain;

import java.time.Duration;
import java.util.Locale;

public enum CandleInterval {
    ONE_MINUTE("1m", Duration.ofMinutes(1)),
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    ONE_DAY("1d", Duration.ofDays(1));

    private final String code;
    private final Duration duration;

    CandleInterval(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public Duration duration() {
        return duration;
    }

    public int unitMinutes() {
        return Math.toIntExact(duration.toMinutes());
    }

    public static CandleInterval fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("interval code must not be blank");
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (CandleInterval interval : values()) {
            if (interval.code.equals(normalized)) {
                return interval;
            }
        }
        throw new IllegalArgumentException("unsupported candle interval: " + code);
    }
}
