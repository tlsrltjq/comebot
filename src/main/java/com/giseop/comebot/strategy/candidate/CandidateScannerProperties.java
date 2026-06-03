package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.exchange.ExchangeMode;
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
    private BigDecimal minLatestCandleTradeAmountKrw = BigDecimal.ZERO;
    private BigDecimal minLatestCandleTradeAmountUsdt = BigDecimal.ZERO;
    private BigDecimal maxDistanceFromHighRate = BigDecimal.ZERO;
    private BigDecimal minDistanceFromHighRate = BigDecimal.ZERO;
    private BigDecimal maxVolumeCooldownRatio = BigDecimal.ZERO;
    private int minConsecutiveBullishCandles = 1;
    private BigDecimal minPriceRecoveryRate = BigDecimal.ZERO;
    private ExchangeSettings upbit = new ExchangeSettings();
    private ExchangeSettings binance = new ExchangeSettings();

    public int getCandleUnitMinutes() {
        return candleUnitMinutes;
    }

    public int getCandleUnitMinutes(ExchangeMode exchange) {
        Integer override = settings(exchange).getCandleUnitMinutes();
        return override == null ? candleUnitMinutes : override;
    }

    public void setCandleUnitMinutes(int candleUnitMinutes) {
        this.candleUnitMinutes = candleUnitMinutes;
    }

    public int getCandleCount() {
        return candleCount;
    }

    public int getCandleCount(ExchangeMode exchange) {
        Integer override = settings(exchange).getCandleCount();
        return override == null ? candleCount : override;
    }

    public void setCandleCount(int candleCount) {
        this.candleCount = candleCount;
    }

    public BigDecimal getMinPriceChangeRate() {
        return minPriceChangeRate;
    }

    public BigDecimal getMinPriceChangeRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinPriceChangeRate();
        return override == null ? minPriceChangeRate : override;
    }

    public void setMinPriceChangeRate(BigDecimal minPriceChangeRate) {
        this.minPriceChangeRate = minPriceChangeRate == null ? new BigDecimal("0.3") : minPriceChangeRate;
    }

    public BigDecimal getMinTradeAmountChangeRate() {
        return minTradeAmountChangeRate;
    }

    public BigDecimal getMinTradeAmountChangeRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinTradeAmountChangeRate();
        return override == null ? minTradeAmountChangeRate : override;
    }

    public void setMinTradeAmountChangeRate(BigDecimal minTradeAmountChangeRate) {
        this.minTradeAmountChangeRate = minTradeAmountChangeRate == null ? new BigDecimal("20") : minTradeAmountChangeRate;
    }

    public BigDecimal getMaxPriceChangeRate() {
        return maxPriceChangeRate;
    }

    public BigDecimal getMaxPriceChangeRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMaxPriceChangeRate();
        return override == null ? maxPriceChangeRate : override;
    }

    public void setMaxPriceChangeRate(BigDecimal maxPriceChangeRate) {
        this.maxPriceChangeRate = maxPriceChangeRate == null ? new BigDecimal("10") : maxPriceChangeRate;
    }

    public BigDecimal getMaxHighLowRangeRate() {
        return maxHighLowRangeRate;
    }

    public BigDecimal getMaxHighLowRangeRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMaxHighLowRangeRate();
        return override == null ? maxHighLowRangeRate : override;
    }

    public void setMaxHighLowRangeRate(BigDecimal maxHighLowRangeRate) {
        this.maxHighLowRangeRate = maxHighLowRangeRate == null ? new BigDecimal("20") : maxHighLowRangeRate;
    }

    public BigDecimal getMinLatestCandleTradeAmountKrw() {
        return minLatestCandleTradeAmountKrw;
    }

    public BigDecimal getMinLatestCandleTradeAmountKrw(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinLatestCandleTradeAmountKrw();
        return override == null ? minLatestCandleTradeAmountKrw : override;
    }

    public void setMinLatestCandleTradeAmountKrw(BigDecimal minLatestCandleTradeAmountKrw) {
        this.minLatestCandleTradeAmountKrw = minLatestCandleTradeAmountKrw == null ? BigDecimal.ZERO : minLatestCandleTradeAmountKrw;
    }

    public BigDecimal getMinLatestCandleTradeAmountUsdt() {
        return minLatestCandleTradeAmountUsdt;
    }

    public BigDecimal getMinLatestCandleTradeAmountUsdt(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinLatestCandleTradeAmountUsdt();
        return override == null ? minLatestCandleTradeAmountUsdt : override;
    }

    public void setMinLatestCandleTradeAmountUsdt(BigDecimal minLatestCandleTradeAmountUsdt) {
        this.minLatestCandleTradeAmountUsdt = minLatestCandleTradeAmountUsdt == null ? BigDecimal.ZERO : minLatestCandleTradeAmountUsdt;
    }

    public BigDecimal getMaxDistanceFromHighRate() {
        return maxDistanceFromHighRate;
    }

    public BigDecimal getMaxDistanceFromHighRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMaxDistanceFromHighRate();
        return override == null ? maxDistanceFromHighRate : override;
    }

    public void setMaxDistanceFromHighRate(BigDecimal maxDistanceFromHighRate) {
        this.maxDistanceFromHighRate = maxDistanceFromHighRate == null ? BigDecimal.ZERO : maxDistanceFromHighRate;
    }

    public BigDecimal getMinDistanceFromHighRate() {
        return minDistanceFromHighRate;
    }

    public void setMinDistanceFromHighRate(BigDecimal minDistanceFromHighRate) {
        this.minDistanceFromHighRate = minDistanceFromHighRate == null ? BigDecimal.ZERO : minDistanceFromHighRate;
    }

    public BigDecimal getMinDistanceFromHighRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinDistanceFromHighRate();
        return override == null ? minDistanceFromHighRate : override;
    }

    public BigDecimal getMaxVolumeCooldownRatio() {
        return maxVolumeCooldownRatio;
    }

    public BigDecimal getMaxVolumeCooldownRatio(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMaxVolumeCooldownRatio();
        return override == null ? maxVolumeCooldownRatio : override;
    }

    public void setMaxVolumeCooldownRatio(BigDecimal maxVolumeCooldownRatio) {
        this.maxVolumeCooldownRatio = maxVolumeCooldownRatio == null ? BigDecimal.ZERO : maxVolumeCooldownRatio;
    }

    public int getMinConsecutiveBullishCandles() {
        return minConsecutiveBullishCandles;
    }

    public int getMinConsecutiveBullishCandles(ExchangeMode exchange) {
        Integer override = settings(exchange).getMinConsecutiveBullishCandles();
        return override == null ? minConsecutiveBullishCandles : override;
    }

    public void setMinConsecutiveBullishCandles(int minConsecutiveBullishCandles) {
        this.minConsecutiveBullishCandles = Math.max(1, minConsecutiveBullishCandles);
    }

    public BigDecimal getMinPriceRecoveryRate() {
        return minPriceRecoveryRate;
    }

    public BigDecimal getMinPriceRecoveryRate(ExchangeMode exchange) {
        BigDecimal override = settings(exchange).getMinPriceRecoveryRate();
        return override == null ? minPriceRecoveryRate : override;
    }

    public void setMinPriceRecoveryRate(BigDecimal minPriceRecoveryRate) {
        this.minPriceRecoveryRate = minPriceRecoveryRate == null ? BigDecimal.ZERO : minPriceRecoveryRate;
    }

    public ExchangeSettings getUpbit() {
        return upbit;
    }

    public void setUpbit(ExchangeSettings upbit) {
        this.upbit = upbit == null ? new ExchangeSettings() : upbit;
    }

    public ExchangeSettings getBinance() {
        return binance;
    }

    public void setBinance(ExchangeSettings binance) {
        this.binance = binance == null ? new ExchangeSettings() : binance;
    }

    private ExchangeSettings settings(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? binance : upbit;
    }

    public static class ExchangeSettings {

        private Integer candleUnitMinutes;
        private Integer candleCount;
        private BigDecimal minPriceChangeRate;
        private BigDecimal minTradeAmountChangeRate;
        private BigDecimal maxPriceChangeRate;
        private BigDecimal maxHighLowRangeRate;
        private BigDecimal minLatestCandleTradeAmountKrw;
        private BigDecimal minLatestCandleTradeAmountUsdt;
        private BigDecimal maxDistanceFromHighRate;
        private BigDecimal minDistanceFromHighRate;
        private BigDecimal maxVolumeCooldownRatio;
        private Integer minConsecutiveBullishCandles;
        private BigDecimal minPriceRecoveryRate;

        public Integer getCandleUnitMinutes() {
            return candleUnitMinutes;
        }

        public void setCandleUnitMinutes(Integer candleUnitMinutes) {
            this.candleUnitMinutes = candleUnitMinutes;
        }

        public Integer getCandleCount() {
            return candleCount;
        }

        public void setCandleCount(Integer candleCount) {
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

        public BigDecimal getMinLatestCandleTradeAmountKrw() {
            return minLatestCandleTradeAmountKrw;
        }

        public void setMinLatestCandleTradeAmountKrw(BigDecimal minLatestCandleTradeAmountKrw) {
            this.minLatestCandleTradeAmountKrw = minLatestCandleTradeAmountKrw;
        }

        public BigDecimal getMinLatestCandleTradeAmountUsdt() {
            return minLatestCandleTradeAmountUsdt;
        }

        public void setMinLatestCandleTradeAmountUsdt(BigDecimal minLatestCandleTradeAmountUsdt) {
            this.minLatestCandleTradeAmountUsdt = minLatestCandleTradeAmountUsdt;
        }

        public BigDecimal getMaxDistanceFromHighRate() {
            return maxDistanceFromHighRate;
        }

        public void setMaxDistanceFromHighRate(BigDecimal maxDistanceFromHighRate) {
            this.maxDistanceFromHighRate = maxDistanceFromHighRate;
        }

        public BigDecimal getMinDistanceFromHighRate() {
            return minDistanceFromHighRate;
        }

        public void setMinDistanceFromHighRate(BigDecimal minDistanceFromHighRate) {
            this.minDistanceFromHighRate = minDistanceFromHighRate;
        }

        public BigDecimal getMaxVolumeCooldownRatio() {
            return maxVolumeCooldownRatio;
        }

        public void setMaxVolumeCooldownRatio(BigDecimal maxVolumeCooldownRatio) {
            this.maxVolumeCooldownRatio = maxVolumeCooldownRatio;
        }

        public Integer getMinConsecutiveBullishCandles() {
            return minConsecutiveBullishCandles;
        }

        public void setMinConsecutiveBullishCandles(Integer minConsecutiveBullishCandles) {
            this.minConsecutiveBullishCandles = minConsecutiveBullishCandles;
        }

        public BigDecimal getMinPriceRecoveryRate() {
            return minPriceRecoveryRate;
        }

        public void setMinPriceRecoveryRate(BigDecimal minPriceRecoveryRate) {
            this.minPriceRecoveryRate = minPriceRecoveryRate;
        }
    }
}
