package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "paper_position")
@IdClass(PaperPositionId.class)
public class PaperPositionEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange;

    @Id
    @Column(nullable = false, length = 50)
    private String market;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "average_buy_price", nullable = false, precision = 38, scale = 18)
    private BigDecimal averageBuyPrice = BigDecimal.ZERO;

    @Column(name = "peak_price", precision = 38, scale = 18)
    private BigDecimal peakPrice;

    protected PaperPositionEntity() {
    }

    private PaperPositionEntity(ExchangeMode exchange, String market, BigDecimal quantity, BigDecimal averageBuyPrice, BigDecimal peakPrice) {
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
        this.market = market;
        this.quantity = quantity == null ? BigDecimal.ZERO : quantity;
        this.averageBuyPrice = averageBuyPrice == null ? BigDecimal.ZERO : averageBuyPrice;
        this.peakPrice = peakPrice;
    }

    public static PaperPositionEntity from(ExchangeMode exchange, PaperPosition position) {
        return new PaperPositionEntity(
                exchange,
                position.market(),
                position.quantity(),
                position.averageBuyPrice(),
                position.peakPrice()
        );
    }

    public PaperPosition toDomain() {
        return new PaperPosition(market, quantity, averageBuyPrice, peakPrice);
    }
}
