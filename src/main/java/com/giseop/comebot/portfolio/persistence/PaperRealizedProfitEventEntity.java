package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperRealizedProfit;
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
@Table(name = "paper_realized_profit_event")
public class PaperRealizedProfitEventEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal profit;

    @Column(name = "realized_at", nullable = false)
    private Instant realizedAt;

    protected PaperRealizedProfitEventEntity() {
    }

    private PaperRealizedProfitEventEntity(String id, ExchangeMode exchange, BigDecimal profit, Instant realizedAt) {
        this.id = id;
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
        this.profit = profit == null ? BigDecimal.ZERO : profit;
        this.realizedAt = realizedAt == null ? Instant.now() : realizedAt;
    }

    public static PaperRealizedProfitEventEntity from(ExchangeMode exchange, PaperRealizedProfit realizedProfit) {
        return new PaperRealizedProfitEventEntity(
                UUID.randomUUID().toString(),
                exchange,
                realizedProfit.profit(),
                realizedProfit.realizedAt()
        );
    }

    public PaperRealizedProfit toDomain() {
        return new PaperRealizedProfit(profit, realizedAt);
    }
}
