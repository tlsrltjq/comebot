package com.giseop.comebot.telegram.sender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.telegram.TelegramProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramNotificationSenderTest {

    @Test
    void sendDoesNotCallApiWhenTelegramIsDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();
        properties.setEnabled(false);

        server.expect(never(), requestTo("https://api.telegram.org/bottoken/sendMessage"));

        TelegramSendResult result = new TelegramNotificationSender(properties, builder.build())
                .sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_DISABLED);
        server.verify();
    }

    @Test
    void sendDoesNotCallApiWhenTelegramIsNotConfigured() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);

        server.expect(never(), requestTo("https://api.telegram.org/bottoken/sendMessage"));

        TelegramSendResult result = new TelegramNotificationSender(properties, builder.build())
                .sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_NOT_CONFIGURED);
        server.verify();
    }

    @Test
    void sendCallsTelegramSendMessageWhenEnabledAndConfigured() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json("{\"chat_id\":\"chat-id\",\"text\":\"body\"}"))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        TelegramSendResult result = new TelegramNotificationSender(properties, builder.build())
                .sendMessageWithResult(message());

        assertThat(result.sent()).isTrue();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.SENT);
        server.verify();
    }

    @Test
    void sendDoesNotExposeBotTokenThroughResponseObject() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        assertThatCode(() -> new TelegramNotificationSender(properties, builder.build()).sendMessage(message()))
                .doesNotThrowAnyException();

        server.verify();
    }

    @Test
    void sendHandlesTelegramApiFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andRespond(withServerError());

        TelegramNotificationSender sender = new TelegramNotificationSender(properties, builder.build());
        TelegramSendResult result = sender.sendMessageWithResult(message());

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_API_FAILED);
        server.verify();
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
}
