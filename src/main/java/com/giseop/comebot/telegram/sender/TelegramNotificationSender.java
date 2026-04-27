package com.giseop.comebot.telegram.sender;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationSender;
import com.giseop.comebot.telegram.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "notification", name = "enabled", havingValue = "true")
public class TelegramNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationSender.class);
    private static final String TELEGRAM_API_BASE_URL = "https://api.telegram.org";

    private final TelegramProperties telegramProperties;
    private final RestClient restClient;

    public TelegramNotificationSender(TelegramProperties telegramProperties, RestClient.Builder restClientBuilder) {
        this(telegramProperties, restClientBuilder.baseUrl(TELEGRAM_API_BASE_URL).build());
    }

    TelegramNotificationSender(TelegramProperties telegramProperties, RestClient restClient) {
        this.telegramProperties = telegramProperties;
        this.restClient = restClient;
    }

    @Override
    public void send(NotificationMessage message) {
        if (!telegramProperties.isEnabled()) {
            log.warn("Telegram notification skipped because telegram.enabled=false");
            return;
        }
        if (!telegramProperties.isConfigured()) {
            log.warn("Telegram notification skipped because telegram is not configured");
            return;
        }

        try {
            restClient.post()
                    .uri("/bot{botToken}/sendMessage", telegramProperties.getBotToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendMessageRequest(telegramProperties.getChatId(), message.body()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Telegram notification send failed: {}", exception.getClass().getSimpleName());
        }
    }

    private record SendMessageRequest(
            String chat_id,
            String text
    ) {
    }
}
