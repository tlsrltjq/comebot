package com.giseop.comebot.history.domain;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingFlowHistory(
        String id,
        ExchangeMode exchange,
        String market,
        BigDecimal currentPrice,
        SignalType signalType,
        String signalReason,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant createdAt
) {
    public TradingFlowHistory(
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
        this(
                id,
                ExchangeMode.UPBIT,
                market,
                currentPrice,
                signalType,
                signalReason,
                orderCreated,
                orderStatus,
                message,
                createdAt
        );
    }

    public TradingFlowHistory {
        exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
    }
}
