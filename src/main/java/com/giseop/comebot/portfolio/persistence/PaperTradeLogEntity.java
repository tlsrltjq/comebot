package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperTradeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paper_trade_log")
public class PaperTradeLogEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange;

    @Column(nullable = false, length = 50)
    private String market;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderSide side;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal price;

    @Column(name = "gross_amount", nullable = false, precision = 38, scale = 18)
    private BigDecimal grossAmount;

    @Column(name = "realized_profit", precision = 38, scale = 18)
    private BigDecimal realizedProfit;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected PaperTradeLogEntity() {
    }

    private PaperTradeLogEntity(
            String id,
            ExchangeMode exchange,
            String market,
            OrderSide side,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal grossAmount,
            BigDecimal realizedProfit,
            Instant executedAt
    ) {
        this.id = id;
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
        this.market = market;
        this.side = side;
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.grossAmount = grossAmount == null ? BigDecimal.ZERO : grossAmount;
        this.realizedProfit = realizedProfit;
        this.executedAt = executedAt == null ? Instant.now() : executedAt;
    }

    public static PaperTradeLogEntity from(ExchangeMode exchange, PaperTradeLog tradeLog) {
        return new PaperTradeLogEntity(
                UUID.randomUUID().toString(),
                exchange,
                tradeLog.market(),
                tradeLog.side(),
                tradeLog.quantity(),
                tradeLog.price(),
                tradeLog.grossAmount(),
                tradeLog.realizedProfit(),
                tradeLog.executedAt()
        );
    }
}
