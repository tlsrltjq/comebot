package com.giseop.comebot.notification;

import com.giseop.comebot.trading.service.TradingFlowResult;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TradingFlowNotificationService {

    private final NotificationSender notificationSender;

    public TradingFlowNotificationService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public NotificationMessage toMessage(TradingFlowResult result) {
        String title = "Trading flow result: " + result.market();
        String body = "signalType=%s, orderCreated=%s, orderStatus=%s, message=%s"
                .formatted(
                        result.signalType(),
                        result.orderCreated(),
                        result.orderStatus(),
                        result.message()
                );
        return new NotificationMessage(title, body, Instant.now());
    }

    public void notify(TradingFlowResult result) {
        notificationSender.send(toMessage(result));
    }
}
