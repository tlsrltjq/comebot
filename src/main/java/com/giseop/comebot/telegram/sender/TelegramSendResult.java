package com.giseop.comebot.telegram.sender;

public record TelegramSendResult(
        boolean sent,
        TelegramSendReason reason
) {
}
