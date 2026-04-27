package com.giseop.comebot.telegram.sender;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationSender;
import com.giseop.comebot.telegram.TelegramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class TelegramNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationSender.class);

    private final TelegramProperties telegramProperties;
    private final TelegramApiClient telegramApiClient;

    public TelegramNotificationSender(TelegramProperties telegramProperties, TelegramApiClient telegramApiClient) {
        this.telegramProperties = telegramProperties;
        this.telegramApiClient = telegramApiClient;
    }

    @Override
    public void send(NotificationMessage message) {
        sendMessage(message);
    }

    public boolean sendMessage(NotificationMessage message) {
        return sendMessageWithResult(message).sent();
    }

    public TelegramSendResult sendMessageWithResult(NotificationMessage message) {
        if (!telegramProperties.isEnabled()) {
            log.warn("Telegram notification skipped because telegram.enabled=false");
            return new TelegramSendResult(false, TelegramSendReason.TELEGRAM_DISABLED);
        }
        if (!telegramProperties.isConfigured()) {
            log.warn("Telegram notification skipped because telegram is not configured");
            return new TelegramSendResult(false, TelegramSendReason.TELEGRAM_NOT_CONFIGURED);
        }

        try {
            telegramApiClient.sendMessage(
                    telegramProperties.getBotToken(),
                    telegramProperties.getChatId(),
                    message.body()
            );
            return new TelegramSendResult(true, TelegramSendReason.SENT);
        } catch (RestClientException exception) {
            log.warn("Telegram notification send failed: {}", exception.getClass().getSimpleName());
            return new TelegramSendResult(false, TelegramSendReason.TELEGRAM_API_FAILED);
        }
    }
}
