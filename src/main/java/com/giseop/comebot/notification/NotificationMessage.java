package com.giseop.comebot.notification;

import java.time.Instant;

public record NotificationMessage(
        String title,
        String body,
        Instant createdAt
) {
}
