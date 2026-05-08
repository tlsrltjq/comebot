package com.giseop.comebot.history.dto;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingFlowHistoryResponse(
        String id,
        String exchange,
        String market,
        BigDecimal currentPrice,
        SignalType signalType,
        String signalReason,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant createdAt
) {
    public TradingFlowHistoryResponse(
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
        this(id, "UPBIT", market, currentPrice, signalType, signalReason, orderCreated, orderStatus, message, createdAt);
    }
}
