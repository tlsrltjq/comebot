package com.giseop.comebot.risk;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk")
public class PositionExitProperties {

    private BigDecimal takeProfitRate = new BigDecimal("5");
    private BigDecimal stopLossRate = new BigDecimal("-3");
    private boolean positionExitEnabled = false;
    private boolean trailingStopEnabled = false;
    private BigDecimal trailingStopActivationRate = new BigDecimal("0.5");
    private BigDecimal trailingStopTrailRate = new BigDecimal("0.3");

    public BigDecimal getTakeProfitRate() {
        return takeProfitRate;
    }

    public void setTakeProfitRate(BigDecimal takeProfitRate) {
        this.takeProfitRate = takeProfitRate == null ? new BigDecimal("5") : takeProfitRate;
    }

    public BigDecimal getStopLossRate() {
        return stopLossRate;
    }

    public void setStopLossRate(BigDecimal stopLossRate) {
        this.stopLossRate = stopLossRate == null ? new BigDecimal("-3") : stopLossRate;
    }

    public boolean isPositionExitEnabled() {
        return positionExitEnabled;
    }

    public void setPositionExitEnabled(boolean positionExitEnabled) {
        this.positionExitEnabled = positionExitEnabled;
    }

    public boolean isTrailingStopEnabled() {
        return trailingStopEnabled;
    }

    public void setTrailingStopEnabled(boolean trailingStopEnabled) {
        this.trailingStopEnabled = trailingStopEnabled;
    }

    public BigDecimal getTrailingStopActivationRate() {
        return trailingStopActivationRate;
    }

    public void setTrailingStopActivationRate(BigDecimal trailingStopActivationRate) {
        this.trailingStopActivationRate = trailingStopActivationRate == null ? new BigDecimal("0.5") : trailingStopActivationRate;
    }

    public BigDecimal getTrailingStopTrailRate() {
        return trailingStopTrailRate;
    }

    public void setTrailingStopTrailRate(BigDecimal trailingStopTrailRate) {
        this.trailingStopTrailRate = trailingStopTrailRate == null ? new BigDecimal("0.3") : trailingStopTrailRate;
    }
}
