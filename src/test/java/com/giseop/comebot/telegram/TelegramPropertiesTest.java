package com.giseop.comebot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramPropertiesTest {

    @Test
    void defaultEnabledIsFalse() {
        TelegramProperties properties = new TelegramProperties();

        assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    void configuredIsFalseWhenTokenOrChatIdIsMissing() {
        TelegramProperties properties = new TelegramProperties();
        properties.setBotToken("token");

        assertThat(properties.isConfigured()).isFalse();
    }

    @Test
    void configuredIsTrueWhenTokenAndChatIdExist() {
        TelegramProperties properties = new TelegramProperties();
        properties.setBotToken("token");
        properties.setChatId("chat-id");

        assertThat(properties.isConfigured()).isTrue();
    }
}
