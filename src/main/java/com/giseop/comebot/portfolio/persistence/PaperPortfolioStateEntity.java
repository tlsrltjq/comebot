package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "paper_portfolio_state")
public class PaperPortfolioStateEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExchangeMode exchange;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal cash = BigDecimal.ZERO;

    @Column(name = "realized_profit", nullable = false, precision = 38, scale = 18)
    private BigDecimal realizedProfit = BigDecimal.ZERO;

    protected PaperPortfolioStateEntity() {
    }

    public PaperPortfolioStateEntity(ExchangeMode exchange, BigDecimal cash, BigDecimal realizedProfit) {
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
        this.cash = cash == null ? BigDecimal.ZERO : cash;
        this.realizedProfit = realizedProfit == null ? BigDecimal.ZERO : realizedProfit;
    }

    public ExchangeMode getExchange() {
        return exchange;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash == null ? BigDecimal.ZERO : cash;
    }

    public BigDecimal getRealizedProfit() {
        return realizedProfit;
    }

    public void setRealizedProfit(BigDecimal realizedProfit) {
        this.realizedProfit = realizedProfit == null ? BigDecimal.ZERO : realizedProfit;
    }
}
