package com.giseop.comebot.strategy.candidate;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.candidate-scanner")
public class CandidateScannerProperties {

    private int candleUnitMinutes = 1;
    private int candleCount = 20;
    private BigDecimal minPriceChangeRate = new BigDecimal("1.5");
    private BigDecimal minTradeAmountChangeRate = BigDecimal.ZERO;

    public int getCandleUnitMinutes() {
        return candleUnitMinutes;
    }

    public void setCandleUnitMinutes(int candleUnitMinutes) {
        this.candleUnitMinutes = candleUnitMinutes;
    }

    public int getCandleCount() {
        return candleCount;
    }

    public void setCandleCount(int candleCount) {
        this.candleCount = candleCount;
    }

    public BigDecimal getMinPriceChangeRate() {
        return minPriceChangeRate;
    }

    public void setMinPriceChangeRate(BigDecimal minPriceChangeRate) {
        this.minPriceChangeRate = minPriceChangeRate == null ? new BigDecimal("1.5") : minPriceChangeRate;
    }

    public BigDecimal getMinTradeAmountChangeRate() {
        return minTradeAmountChangeRate;
    }

    public void setMinTradeAmountChangeRate(BigDecimal minTradeAmountChangeRate) {
        this.minTradeAmountChangeRate = minTradeAmountChangeRate == null ? BigDecimal.ZERO : minTradeAmountChangeRate;
    }
}
