package com.giseop.comebot.telegram.inbound;

public record TelegramUpdate(
        long updateId,
        String text,
        String callbackData
) {

    public static TelegramUpdate message(long updateId, String text) {
        return new TelegramUpdate(updateId, text, null);
    }

    public static TelegramUpdate callback(long updateId, String callbackData) {
        return new TelegramUpdate(updateId, null, callbackData);
    }
}
