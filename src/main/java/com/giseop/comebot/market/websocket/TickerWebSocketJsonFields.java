package com.giseop.comebot.market.websocket;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TickerWebSocketJsonFields {

    private TickerWebSocketJsonFields() {
    }

    static Optional<String> stringField(String payload, String field) {
        if (payload == null || field == null || field.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(payload);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    static Optional<BigDecimal> decimalField(String payload, String field) {
        if (payload == null || field == null || field.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"?([-0-9.]+)\"?")
                .matcher(payload);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(matcher.group(1)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
