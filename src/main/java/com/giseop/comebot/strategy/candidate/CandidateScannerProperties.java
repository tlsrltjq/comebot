package com.giseop.comebot.strategy.candidate;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.candidate-scanner")
public class CandidateScannerProperties {

    private int candleUnitMinutes = 1;
    private int candleCount = 5;
    private BigDecimal minPriceChangeRate = new BigDecimal("0.3");
    private BigDecimal minTradeAmountChangeRate = new BigDecimal("20");
    private BigDecimal maxPriceChangeRate = new BigDecimal("10");
    private BigDecimal maxHighLowRangeRate = new BigDecimal("20");

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
        this.minPriceChangeRate = minPriceChangeRate == null ? new BigDecimal("0.3") : minPriceChangeRate;
    }

    public BigDecimal getMinTradeAmountChangeRate() {
        return minTradeAmountChangeRate;
    }

    public void setMinTradeAmountChangeRate(BigDecimal minTradeAmountChangeRate) {
        this.minTradeAmountChangeRate = minTradeAmountChangeRate == null ? new BigDecimal("20") : minTradeAmountChangeRate;
    }

    public BigDecimal getMaxPriceChangeRate() {
        return maxPriceChangeRate;
    }

    public void setMaxPriceChangeRate(BigDecimal maxPriceChangeRate) {
        this.maxPriceChangeRate = maxPriceChangeRate == null ? new BigDecimal("10") : maxPriceChangeRate;
    }

    public BigDecimal getMaxHighLowRangeRate() {
        return maxHighLowRangeRate;
    }

    public void setMaxHighLowRangeRate(BigDecimal maxHighLowRangeRate) {
        this.maxHighLowRangeRate = maxHighLowRangeRate == null ? new BigDecimal("20") : maxHighLowRangeRate;
    }
}
