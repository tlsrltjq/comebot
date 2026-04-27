package com.giseop.comebot.telegram.inbound;

import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class TelegramPollingRunner {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingRunner.class);

    private final TelegramProperties telegramProperties;
    private final TelegramInboundProperties telegramInboundProperties;
    private final TelegramUpdateClient telegramUpdateClient;
    private final TelegramCommandService telegramCommandService;
    private final TelegramApiClient telegramApiClient;
    private long nextOffset = 0;

    public TelegramPollingRunner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties,
            TelegramUpdateClient telegramUpdateClient,
            TelegramCommandService telegramCommandService,
            TelegramApiClient telegramApiClient
    ) {
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
        this.telegramUpdateClient = telegramUpdateClient;
        this.telegramCommandService = telegramCommandService;
        this.telegramApiClient = telegramApiClient;
    }

    @Scheduled(fixedDelayString = "${telegram.inbound.fixed-delay-ms:3000}")
    public void poll() {
        if (!shouldPoll()) {
            return;
        }

        try {
            List<TelegramUpdate> updates = telegramUpdateClient.getUpdates(telegramProperties.getBotToken(), nextOffset);
            for (TelegramUpdate update : updates) {
                nextOffset = Math.max(nextOffset, update.updateId() + 1);
                handleUpdate(update);
            }
        } catch (RestClientException exception) {
            log.warn("Telegram inbound polling failed: {}", exception.getClass().getSimpleName());
        } catch (RuntimeException exception) {
            log.warn("Telegram inbound polling failed: {}", exception.getClass().getSimpleName());
        }
    }

    private boolean shouldPoll() {
        return telegramProperties.isEnabled()
                && telegramProperties.isConfigured()
                && telegramInboundProperties.isEnabled();
    }

    private void handleUpdate(TelegramUpdate update) {
        try {
            if (!isAllowedChat(update.chatId())) {
                return;
            }
            if (update.callbackData() != null && !update.callbackData().isBlank()) {
                telegramCommandService.handleCallback(update.callbackData());
                return;
            }
            telegramCommandService.handle(update.text());
        } catch (RuntimeException exception) {
            log.warn("Telegram command handling failed: {}", exception.getClass().getSimpleName());
        } finally {
            answerCallbackQuery(update);
        }
    }

    private boolean isAllowedChat(String chatId) {
        return chatId != null && chatId.equals(telegramProperties.getChatId());
    }

    private void answerCallbackQuery(TelegramUpdate update) {
        if (update.callbackQueryId() == null || update.callbackQueryId().isBlank()) {
            return;
        }
        try {
            telegramApiClient.answerCallbackQuery(telegramProperties.getBotToken(), update.callbackQueryId());
        } catch (RuntimeException exception) {
            log.warn("Telegram answerCallbackQuery failed: {}", exception.getClass().getSimpleName());
        }
    }
}
