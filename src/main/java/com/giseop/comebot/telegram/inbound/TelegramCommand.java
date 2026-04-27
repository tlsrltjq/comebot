package com.giseop.comebot.telegram.inbound;

public record TelegramCommand(
        TelegramCommandType type,
        String market
) {
}
