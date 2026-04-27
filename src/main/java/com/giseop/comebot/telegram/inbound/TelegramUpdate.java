package com.giseop.comebot.telegram.inbound;

public record TelegramUpdate(
        long updateId,
        String text
) {
}
