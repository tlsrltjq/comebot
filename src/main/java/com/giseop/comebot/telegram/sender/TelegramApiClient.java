package com.giseop.comebot.telegram.sender;

public interface TelegramApiClient {

    void sendMessage(String botToken, String chatId, String text);
}
