package com.giseop.comebot.telegram.controller;

import com.giseop.comebot.telegram.dto.TelegramTestMessageRequest;
import com.giseop.comebot.telegram.dto.TelegramTestMessageResponse;
import com.giseop.comebot.telegram.service.TelegramTestMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelegramTestMessageController {

    private final TelegramTestMessageService telegramTestMessageService;

    public TelegramTestMessageController(TelegramTestMessageService telegramTestMessageService) {
        this.telegramTestMessageService = telegramTestMessageService;
    }

    @PostMapping("/api/telegram/test-message")
    public ResponseEntity<TelegramTestMessageResponse> sendTestMessage(
            @RequestBody(required = false) TelegramTestMessageRequest request
    ) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        boolean sent = telegramTestMessageService.sendTestMessage(request.message());
        return ResponseEntity.ok(new TelegramTestMessageResponse(sent, request.message()));
    }
}
