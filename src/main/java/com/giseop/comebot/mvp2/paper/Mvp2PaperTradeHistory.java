package com.giseop.comebot.mvp2.paper;

import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.mvp2.exchange.Exchange;
import java.math.BigDecimal;
import java.time.Instant;

public record Mvp2PaperTradeHistory(
        Exchange exchange,
        String symbol,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        OrderStatus status,
        String reason,
        String message,
        Instant createdAt
) {
}
