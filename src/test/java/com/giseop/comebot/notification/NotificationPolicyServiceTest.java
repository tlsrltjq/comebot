package com.giseop.comebot.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationPolicyServiceTest {

    @Test
    void shouldNotNotifyHoldByDefault() {
        NotificationProperties properties = new NotificationProperties();

        assertThat(new NotificationPolicyService(properties).shouldNotify(result(SignalType.HOLD, null))).isFalse();
    }

    @Test
    void shouldNotifyHoldWhenEnabledByPolicy() {
        NotificationProperties properties = new NotificationProperties();
        properties.setSendHold(true);

        assertThat(new NotificationPolicyService(properties).shouldNotify(result(SignalType.HOLD, null))).isTrue();
    }

    @Test
    void shouldFollowFilledPolicy() {
        NotificationProperties properties = new NotificationProperties();
        NotificationPolicyService service = new NotificationPolicyService(properties);

        assertThat(service.shouldNotify(result(SignalType.BUY, OrderStatus.FILLED))).isTrue();

        properties.setSendFilled(false);

        assertThat(service.shouldNotify(result(SignalType.BUY, OrderStatus.FILLED))).isFalse();
    }

    @Test
    void shouldFollowRejectedPolicy() {
        NotificationProperties properties = new NotificationProperties();
        NotificationPolicyService service = new NotificationPolicyService(properties);

        assertThat(service.shouldNotify(result(SignalType.BUY, OrderStatus.REJECTED))).isTrue();

        properties.setSendRejected(false);

        assertThat(service.shouldNotify(result(SignalType.BUY, OrderStatus.REJECTED))).isFalse();
    }

    private TradingFlowResult result(SignalType signalType, OrderStatus orderStatus) {
        return new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("100"),
                signalType,
                "reason",
                orderStatus != null,
                orderStatus,
                "message",
                Instant.now()
        );
    }
}
