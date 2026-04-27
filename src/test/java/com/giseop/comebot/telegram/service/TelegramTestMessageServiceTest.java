package com.giseop.comebot.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import com.giseop.comebot.telegram.sender.TelegramSendReason;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class TelegramTestMessageServiceTest {

    @Test
    void sendTestMessageReturnsFalseWhenTelegramIsDisabled() {
        TelegramProperties properties = configuredProperties();
        properties.setEnabled(false);
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramTestMessageResult result = service(properties, apiClient).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_DISABLED);
        assertThat(apiClient.callCount).isZero();
    }

    @Test
    void sendTestMessageReturnsFalseWhenTelegramIsNotConfigured() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramTestMessageResult result = service(properties, apiClient).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_NOT_CONFIGURED);
        assertThat(apiClient.callCount).isZero();
    }

    @Test
    void sendTestMessageReturnsFalseWhenTelegramSendFails() {
        TelegramProperties properties = configuredProperties();
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();
        apiClient.fail = true;

        TelegramTestMessageResult result = service(properties, apiClient).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_API_FAILED);
        assertThat(apiClient.callCount).isEqualTo(1);
    }

    @Test
    void sendTestMessageReturnsTrueWhenTelegramSendSucceeds() {
        TelegramProperties properties = configuredProperties();
        RecordingTelegramApiClient apiClient = new RecordingTelegramApiClient();

        TelegramTestMessageResult result = service(properties, apiClient).sendTestMessage("hello");

        assertThat(result.sent()).isTrue();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.SENT);
        assertThat(apiClient.callCount).isEqualTo(1);
    }

    private TelegramTestMessageService service(TelegramProperties properties, TelegramApiClient apiClient) {
        return new TelegramTestMessageService(new TelegramNotificationSender(properties, apiClient));
    }

    private TelegramProperties configuredProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }

    private static class RecordingTelegramApiClient implements TelegramApiClient {

        private int callCount;
        private boolean fail;

        @Override
        public void sendMessage(String botToken, String chatId, String text) {
            callCount++;
            if (fail) {
                throw new RestClientException("failed") {
                };
            }
        }
    }
}
