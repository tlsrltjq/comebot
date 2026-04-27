package com.giseop.comebot.telegram.controller;

import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.dto.TelegramStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelegramStatusController {

    private final TelegramProperties telegramProperties;

    public TelegramStatusController(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
    }

    @GetMapping("/api/telegram/status")
    public TelegramStatusResponse getStatus() {
        return new TelegramStatusResponse(
                telegramProperties.isEnabled(),
                telegramProperties.isConfigured()
        );
    }
}
