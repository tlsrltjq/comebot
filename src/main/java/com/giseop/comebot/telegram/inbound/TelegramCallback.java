package com.giseop.comebot.telegram.inbound;

public record TelegramCallback(
        TelegramCallbackType type,
        String market
) {
}
