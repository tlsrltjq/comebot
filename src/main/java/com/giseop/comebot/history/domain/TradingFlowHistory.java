package com.giseop.comebot.history.domain;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingFlowHistory(
        String id,
        String market,
        BigDecimal currentPrice,
        SignalType signalType,
        String signalReason,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant createdAt
) {
}
