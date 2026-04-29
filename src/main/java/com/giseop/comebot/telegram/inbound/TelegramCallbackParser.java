package com.giseop.comebot.telegram.inbound;

import org.springframework.stereotype.Component;

@Component
public class TelegramCallbackParser {

    public TelegramCallback parse(String data) {
        if (data == null || data.isBlank()) {
            return new TelegramCallback(TelegramCallbackType.UNKNOWN, null);
        }

        String normalized = data.trim();
        if ("HELP".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.HELP, null);
        }
        if ("STATUS".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.STATUS, null);
        }
        if ("PORTFOLIO".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.PORTFOLIO, null);
        }
        if ("POSITIONS".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.POSITIONS, null);
        }
        if ("RISK".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.RISK, null);
        }
        if ("SAFETY".equals(normalized)) {
            return new TelegramCallback(TelegramCallbackType.SAFETY, null);
        }
        if (normalized.startsWith("RUN:")) {
            return new TelegramCallback(TelegramCallbackType.RUN, valueAfterPrefix(normalized, "RUN:"));
        }
        if (normalized.startsWith("HISTORY:")) {
            return new TelegramCallback(TelegramCallbackType.HISTORY, valueAfterPrefix(normalized, "HISTORY:"));
        }
        return new TelegramCallback(TelegramCallbackType.UNKNOWN, null);
    }

    private String valueAfterPrefix(String data, String prefix) {
        String value = data.substring(prefix.length());
        return value.isBlank() ? null : value;
    }
}
