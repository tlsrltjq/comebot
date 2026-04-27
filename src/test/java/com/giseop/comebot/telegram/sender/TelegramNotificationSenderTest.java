package com.giseop.comebot.telegram.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.telegram.TelegramProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class TelegramNotificationSenderTest {

    @Test
    void sendDoesNotCallApiWhenTelegramIsDisabled() {
        TelegramProperties properties = configuredProperties();
        properties.setEnabled(false);
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramSendResult result = new TelegramNotificationSender(properties, apiClient)
                .sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_DISABLED);
        assertThat(apiClient.callCount).isZero();
    }

    @Test
    void sendDoesNotCallApiWhenTelegramIsNotConfigured() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramSendResult result = new TelegramNotificationSender(properties, apiClient)
                .sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_NOT_CONFIGURED);
        assertThat(apiClient.callCount).isZero();
    }

    @Test
    void sendCallsTelegramSendMessageWhenEnabledAndConfigured() {
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramSendResult result = new TelegramNotificationSender(configuredProperties(), apiClient)
                .sendMessageWithResult(message());

        assertThat(result.sent()).isTrue();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.SENT);
        assertThat(apiClient.callCount).isEqualTo(1);
        assertThat(apiClient.botToken).isEqualTo("token");
        assertThat(apiClient.chatId).isEqualTo("chat-id");
        assertThat(apiClient.text).isEqualTo("body");
    }

    @Test
    void sendDoesNotExposeBotTokenThroughResponseObject() {
        TelegramSendResult result = new TelegramNotificationSender(
                configuredProperties(),
                new RecordingTelegramApiClient()
        ).sendMessageWithResult(message());

        assertThat(result.toString()).doesNotContain("token");
        assertThat(result.toString()).doesNotContain("chat-id");
    }

    @Test
    void sendHandlesTelegramApiFailure() {
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();
        apiClient.fail = true;

        TelegramSendResult result = new TelegramNotificationSender(configuredProperties(), apiClient)
                .sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_API_FAILED);
    }

    @Test
    void sendDoesNotThrowWhenTelegramApiFails() {
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();
        apiClient.fail = true;

        assertThatCode(() -> new TelegramNotificationSender(configuredProperties(), apiClient).send(message()))
                .doesNotThrowAnyException();
    }

    private TelegramProperties configuredProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }

    private NotificationMessage message() {
        return new NotificationMessage("title", "body", Instant.now());
    }

    private static class RecordingTelegramApiClient implements TelegramApiClient {

        private int callCount;
        private boolean fail;
        private String botToken;
        private String chatId;
        private String text;

        @Override
        public void sendMessage(String botToken, String chatId, String text) {
            callCount++;
            this.botToken = botToken;
            this.chatId = chatId;
            this.text = text;
            if (fail) {
                throw new RestClientException("failed") {
                };
            }
        }
    }
}
