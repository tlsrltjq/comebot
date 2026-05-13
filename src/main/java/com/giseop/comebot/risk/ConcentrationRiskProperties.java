package com.giseop.comebot.risk;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk.concentration")
public class ConcentrationRiskProperties {

    private boolean enabled = false;
    private BigDecimal upbitWarningExposureRate = new BigDecimal("7");
    private BigDecimal upbitBlockExposureRate = new BigDecimal("10");
    private BigDecimal binanceWarningExposureRate = new BigDecimal("25");
    private BigDecimal binanceBlockExposureRate = new BigDecimal("40");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getUpbitWarningExposureRate() {
        return upbitWarningExposureRate;
    }

    public void setUpbitWarningExposureRate(BigDecimal upbitWarningExposureRate) {
        this.upbitWarningExposureRate = upbitWarningExposureRate == null ? new BigDecimal("7") : upbitWarningExposureRate;
    }

    public BigDecimal getUpbitBlockExposureRate() {
        return upbitBlockExposureRate;
    }

    public void setUpbitBlockExposureRate(BigDecimal upbitBlockExposureRate) {
        this.upbitBlockExposureRate = upbitBlockExposureRate == null ? new BigDecimal("10") : upbitBlockExposureRate;
    }

    public BigDecimal getBinanceWarningExposureRate() {
        return binanceWarningExposureRate;
    }

    public void setBinanceWarningExposureRate(BigDecimal binanceWarningExposureRate) {
        this.binanceWarningExposureRate = binanceWarningExposureRate == null ? new BigDecimal("25") : binanceWarningExposureRate;
    }

    public BigDecimal getBinanceBlockExposureRate() {
        return binanceBlockExposureRate;
    }

    public void setBinanceBlockExposureRate(BigDecimal binanceBlockExposureRate) {
        this.binanceBlockExposureRate = binanceBlockExposureRate == null ? new BigDecimal("40") : binanceBlockExposureRate;
    }

    public BigDecimal warningExposureRate(com.giseop.comebot.exchange.ExchangeMode exchange) {
        return exchange == com.giseop.comebot.exchange.ExchangeMode.BINANCE ? binanceWarningExposureRate : upbitWarningExposureRate;
    }

    public BigDecimal blockExposureRate(com.giseop.comebot.exchange.ExchangeMode exchange) {
        return exchange == com.giseop.comebot.exchange.ExchangeMode.BINANCE ? binanceBlockExposureRate : upbitBlockExposureRate;
    }
}
