package com.giseop.comebot.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingFlowNotificationServiceTest {

    @Test
    void toMessageConvertsFilledResult() {
        NotificationMessage message = notificationService().toMessage(result(SignalType.BUY, true, OrderStatus.FILLED));

        assertThat(message.title()).contains("KRW-BTC");
        assertThat(message.body()).contains("signalType=BUY");
        assertThat(message.body()).contains("orderStatus=FILLED");
    }

    @Test
    void toMessageConvertsHoldResult() {
        NotificationMessage message = notificationService().toMessage(result(SignalType.HOLD, false, null));

        assertThat(message.body()).contains("signalType=HOLD");
        assertThat(message.body()).contains("orderCreated=false");
    }

    @Test
    void toMessageConvertsRejectedResult() {
        NotificationMessage message = notificationService().toMessage(result(SignalType.BUY, true, OrderStatus.REJECTED));

        assertThat(message.body()).contains("orderStatus=REJECTED");
    }

    @Test
    void loggingNotificationSenderCanReceiveMessage() {
        LoggingNotificationSender sender = new LoggingNotificationSender();
        NotificationMessage message = new NotificationMessage("title", "body", Instant.now());

        sender.send(message);

        assertThat(message.title()).isEqualTo("title");
    }

    @Test
    void notificationFailureDoesNotChangeTradingResult() {
        TradingFlowResult result = result(SignalType.BUY, true, OrderStatus.FILLED);
        TradingFlowNotificationService service = new TradingFlowNotificationService(message -> {
            throw new IllegalStateException("notification failed");
        });

        assertThatThrownBy(() -> service.notify(result))
                .isInstanceOf(IllegalStateException.class);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void notifySendsConvertedMessage() {
        RecordingNotificationSender sender = new RecordingNotificationSender();
        TradingFlowNotificationService service = new TradingFlowNotificationService(sender);

        service.notify(result(SignalType.BUY, true, OrderStatus.FILLED));

        assertThat(sender.messages).hasSize(1);
        assertThat(sender.messages.getFirst().body()).contains("FILLED");
    }

    private TradingFlowNotificationService notificationService() {
        return new TradingFlowNotificationService(new RecordingNotificationSender());
    }

    private TradingFlowResult result(SignalType signalType, boolean orderCreated, OrderStatus orderStatus) {
        return new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("100"),
                signalType,
                "test signal",
                orderCreated,
                orderStatus,
                orderStatus == null ? "No order created" : "Paper trading order result",
                Instant.now()
        );
    }

    private static class RecordingNotificationSender implements NotificationSender {

        private final List<NotificationMessage> messages = new ArrayList<>();

        @Override
        public void send(NotificationMessage message) {
            messages.add(message);
        }
    }
}
