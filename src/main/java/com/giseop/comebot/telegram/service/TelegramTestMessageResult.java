package com.giseop.comebot.telegram.service;

import com.giseop.comebot.telegram.sender.TelegramSendReason;

public record TelegramTestMessageResult(
        boolean sent,
        TelegramSendReason reason
) {
}
