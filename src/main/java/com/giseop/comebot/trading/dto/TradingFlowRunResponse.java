package com.giseop.comebot.trading.dto;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.time.Instant;

public record TradingFlowRunResponse(
        String market,
        SignalType signalType,
        String signalReason,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant executedAt
) {
}
