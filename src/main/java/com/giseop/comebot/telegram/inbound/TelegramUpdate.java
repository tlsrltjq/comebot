package com.giseop.comebot.telegram.inbound;

public record TelegramUpdate(
        long updateId,
        String text,
        String callbackData,
        String chatId,
        String callbackQueryId
) {

    public static TelegramUpdate message(long updateId, String text) {
        return message(updateId, text, null);
    }

    public static TelegramUpdate message(long updateId, String text, String chatId) {
        return new TelegramUpdate(updateId, text, null, chatId, null);
    }

    public static TelegramUpdate callback(long updateId, String callbackData) {
        return callback(updateId, callbackData, null, null);
    }

    public static TelegramUpdate callback(long updateId, String callbackData, String chatId, String callbackQueryId) {
        return new TelegramUpdate(updateId, null, callbackData, chatId, callbackQueryId);
    }
}
