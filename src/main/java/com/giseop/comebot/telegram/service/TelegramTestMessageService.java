package com.giseop.comebot.telegram.service;

import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TelegramTestMessageService {

    private final TelegramNotificationSender telegramNotificationSender;

    public TelegramTestMessageService(TelegramNotificationSender telegramNotificationSender) {
        this.telegramNotificationSender = telegramNotificationSender;
    }

    public boolean sendTestMessage(String message) {
        return telegramNotificationSender.sendMessage(new NotificationMessage(
                "Telegram test message",
                message,
                Instant.now()
        ));
    }
}
