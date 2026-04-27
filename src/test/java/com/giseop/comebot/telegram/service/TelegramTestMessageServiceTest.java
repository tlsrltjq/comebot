package com.giseop.comebot.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import com.giseop.comebot.telegram.sender.TelegramSendReason;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramTestMessageServiceTest {

    @Test
    void sendTestMessageReturnsFalseWhenTelegramIsDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();
        properties.setEnabled(false);

        server.expect(never(), requestTo("https://api.telegram.org/bottoken/sendMessage"));

        TelegramTestMessageResult result = service(properties, builder).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_DISABLED);
        server.verify();
    }

    @Test
    void sendTestMessageReturnsFalseWhenTelegramIsNotConfigured() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);

        server.expect(never(), requestTo("https://api.telegram.org/bottoken/sendMessage"));

        TelegramTestMessageResult result = service(properties, builder).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_NOT_CONFIGURED);
        server.verify();
    }

    @Test
    void sendTestMessageReturnsFalseWhenTelegramSendFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andRespond(withServerError());

        TelegramTestMessageResult result = service(properties, builder).sendTestMessage("hello");

        assertThat(result.sent()).isFalse();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.TELEGRAM_API_FAILED);
        server.verify();
    }

    @Test
    void sendTestMessageReturnsTrueWhenTelegramSendSucceeds() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.telegram.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TelegramProperties properties = configuredProperties();

        server.expect(once(), requestTo("https://api.telegram.org/bottoken/sendMessage"))
                .andRespond(withSuccess("{}", APPLICATION_JSON));

        TelegramTestMessageResult result = service(properties, builder).sendTestMessage("hello");

        assertThat(result.sent()).isTrue();
        assertThat(result.reason()).isEqualTo(TelegramSendReason.SENT);
        server.verify();
    }

    private TelegramTestMessageService service(TelegramProperties properties, RestClient.Builder builder) {
        return new TelegramTestMessageService(new TelegramNotificationSender(properties, builder.build()));
    }

    private TelegramProperties configuredProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }
}
