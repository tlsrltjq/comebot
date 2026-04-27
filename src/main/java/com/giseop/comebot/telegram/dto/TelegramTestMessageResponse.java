package com.giseop.comebot.telegram.dto;

public record TelegramTestMessageResponse(
        boolean sent,
        String message
) {
}
