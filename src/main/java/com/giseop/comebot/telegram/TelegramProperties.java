package com.giseop.comebot.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private boolean enabled = false;
    private String botToken;
    private String chatId;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public boolean isConfigured() {
        return hasText(botToken) && hasText(chatId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
