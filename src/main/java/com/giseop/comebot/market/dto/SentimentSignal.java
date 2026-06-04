package com.giseop.comebot.market.dto;

public record SentimentSignal(
        String name,
        String value,
        String note,
        int score,
        String direction
) {
    public static SentimentSignal of(String name, String value, String note, int score) {
        String direction = score > 0 ? "BULLISH" : score < 0 ? "BEARISH" : "NEUTRAL";
        return new SentimentSignal(name, value, note, score, direction);
    }

    public static SentimentSignal unavailable(String name, String reason) {
        return new SentimentSignal(name, "N/A", reason, 0, "NEUTRAL");
    }
}
