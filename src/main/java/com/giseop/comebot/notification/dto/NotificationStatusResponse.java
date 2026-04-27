package com.giseop.comebot.notification.dto;

public record NotificationStatusResponse(
        boolean enabled,
        boolean sendHold,
        boolean sendFilled,
        boolean sendRejected
) {
}
