package com.giseop.comebot.mvp2.paper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mvp2.paper")
public class Mvp2PaperTradingProperties {

    private BigDecimal initialCash = new BigDecimal("1000");
    private BigDecimal orderAmount = new BigDecimal("10");
    private BigDecimal takeProfitRate = new BigDecimal("1.5");
    private BigDecimal stopLossRate = new BigDecimal("-0.7");
    private int candleUnitMinutes = 1;
    private int candleCount = 5;
    private BigDecimal minPriceChangeRate = new BigDecimal("0.3");
    private BigDecimal minTradeAmountChangeRate = new BigDecimal("20");
    private BigDecimal maxPriceChangeRate = new BigDecimal("10");
    private BigDecimal maxHighLowRangeRate = new BigDecimal("20");
    private List<String> binanceSymbols = new ArrayList<>(List.of("BTCUSDT", "ETHUSDT", "XRPUSDT", "SOLUSDT"));
    private boolean binanceSchedulerEnabled = false;
    private long binanceSchedulerFixedDelayMs = 30000;

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getTakeProfitRate() {
        return takeProfitRate;
    }

    public void setTakeProfitRate(BigDecimal takeProfitRate) {
        this.takeProfitRate = takeProfitRate;
    }

    public BigDecimal getStopLossRate() {
        return stopLossRate;
    }

    public void setStopLossRate(BigDecimal stopLossRate) {
        this.stopLossRate = stopLossRate;
    }

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
        this.minPriceChangeRate = minPriceChangeRate;
    }

    public BigDecimal getMinTradeAmountChangeRate() {
        return minTradeAmountChangeRate;
    }

    public void setMinTradeAmountChangeRate(BigDecimal minTradeAmountChangeRate) {
        this.minTradeAmountChangeRate = minTradeAmountChangeRate;
    }

    public BigDecimal getMaxPriceChangeRate() {
        return maxPriceChangeRate;
    }

    public void setMaxPriceChangeRate(BigDecimal maxPriceChangeRate) {
        this.maxPriceChangeRate = maxPriceChangeRate;
    }

    public BigDecimal getMaxHighLowRangeRate() {
        return maxHighLowRangeRate;
    }

    public void setMaxHighLowRangeRate(BigDecimal maxHighLowRangeRate) {
        this.maxHighLowRangeRate = maxHighLowRangeRate;
    }

    public List<String> getBinanceSymbols() {
        return binanceSymbols;
    }

    public void setBinanceSymbols(List<String> binanceSymbols) {
        this.binanceSymbols = binanceSymbols;
    }

    public boolean isBinanceSchedulerEnabled() {
        return binanceSchedulerEnabled;
    }

    public void setBinanceSchedulerEnabled(boolean binanceSchedulerEnabled) {
        this.binanceSchedulerEnabled = binanceSchedulerEnabled;
    }

    public long getBinanceSchedulerFixedDelayMs() {
        return binanceSchedulerFixedDelayMs;
    }

    public void setBinanceSchedulerFixedDelayMs(long binanceSchedulerFixedDelayMs) {
        this.binanceSchedulerFixedDelayMs = binanceSchedulerFixedDelayMs;
    }
}
