package com.giseop.comebot.history.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.strategy.domain.SignalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trading_flow_history")
public class TradingFlowHistoryEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange = ExchangeMode.UPBIT;

    @Column(name = "current_price", precision = 19, scale = 8)
    private BigDecimal currentPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", length = 20)
    private SignalType signalType;

    @Column(name = "signal_reason", length = 500)
    private String signalReason;

    @Column(name = "order_created", nullable = false)
    private boolean orderCreated;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 20)
    private OrderStatus orderStatus;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TradingFlowHistoryEntity() {
    }

    private TradingFlowHistoryEntity(
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
        this.id = id;
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
        this.market = market;
        this.currentPrice = currentPrice;
        this.signalType = signalType;
        this.signalReason = signalReason;
        this.orderCreated = orderCreated;
        this.orderStatus = orderStatus;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static TradingFlowHistoryEntity from(TradingFlowHistory history) {
        return new TradingFlowHistoryEntity(
                history.id(),
                history.exchange(),
                history.market(),
                history.currentPrice(),
                history.signalType(),
                history.signalReason(),
                history.orderCreated(),
                history.orderStatus(),
                history.message(),
                history.createdAt()
        );
    }

    public TradingFlowHistory toDomain() {
        return new TradingFlowHistory(
                id,
                exchange == null ? ExchangeMode.UPBIT : exchange,
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
}
