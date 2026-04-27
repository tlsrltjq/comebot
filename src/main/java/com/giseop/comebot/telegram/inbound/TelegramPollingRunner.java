package com.giseop.comebot.telegram.inbound;

import com.giseop.comebot.telegram.TelegramProperties;
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
    private long nextOffset = 0;

    public TelegramPollingRunner(
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties,
            TelegramUpdateClient telegramUpdateClient,
            TelegramCommandService telegramCommandService
    ) {
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
        this.telegramUpdateClient = telegramUpdateClient;
        this.telegramCommandService = telegramCommandService;
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
            telegramCommandService.handle(update.text());
        } catch (RuntimeException exception) {
            log.warn("Telegram command handling failed: {}", exception.getClass().getSimpleName());
        }
    }
}
