package com.giseop.comebot.notification.controller;

import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.dto.NotificationStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationStatusController {

    private final NotificationProperties notificationProperties;

    public NotificationStatusController(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    @GetMapping("/api/notifications/status")
    public NotificationStatusResponse getStatus() {
        return new NotificationStatusResponse(notificationProperties.isEnabled());
    }
}
