package com.giseop.comebot.telegram.dto;

public record TelegramStatusResponse(
        boolean enabled,
        boolean configured
) {
}
