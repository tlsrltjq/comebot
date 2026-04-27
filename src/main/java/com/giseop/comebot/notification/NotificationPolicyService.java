package com.giseop.comebot.notification;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import org.springframework.stereotype.Service;

@Service
public class NotificationPolicyService {

    private final NotificationProperties notificationProperties;

    public NotificationPolicyService(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    public boolean shouldNotify(TradingFlowResult result) {
        if (result.signalType() == SignalType.HOLD) {
            return notificationProperties.isSendHold();
        }
        if (result.orderStatus() == OrderStatus.FILLED) {
            return notificationProperties.isSendFilled();
        }
        if (result.orderStatus() == OrderStatus.REJECTED) {
            return notificationProperties.isSendRejected();
        }
        return false;
    }
}
