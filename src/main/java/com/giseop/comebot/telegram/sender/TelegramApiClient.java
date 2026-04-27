package com.giseop.comebot.telegram.sender;

public interface TelegramApiClient {

    void sendMessage(String botToken, String chatId, String text);

    default void sendMessage(String botToken, String chatId, String text, Object replyMarkup) {
        sendMessage(botToken, chatId, text);
    }

    default void answerCallbackQuery(String botToken, String callbackQueryId) {
    }
}
