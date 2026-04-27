package com.giseop.comebot.trading.service;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.time.Instant;

public record TradingFlowResult(
        String market,
        SignalType signalType,
        String signalReason,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant executedAt
) {
}
