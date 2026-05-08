package com.giseop.comebot.portfolio.dto;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.strategy.domain.SignalType;
import java.time.Instant;

public record SelectedPaperSellResultResponse(
        String market,
        SignalType signalType,
        boolean orderCreated,
        OrderStatus orderStatus,
        String message,
        Instant executedAt
) {
}
