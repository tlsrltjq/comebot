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
}
