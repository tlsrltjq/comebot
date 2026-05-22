package com.giseop.comebot.strategy.service;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.entry")
public class StrategyEntryProperties {

    private boolean preventReentryWithPosition = false;
    private BigDecimal minReentryProfitRate = BigDecimal.ZERO;

    public boolean isPreventReentryWithPosition() {
        return preventReentryWithPosition;
    }

    public void setPreventReentryWithPosition(boolean preventReentryWithPosition) {
        this.preventReentryWithPosition = preventReentryWithPosition;
    }

    public BigDecimal getMinReentryProfitRate() {
        return minReentryProfitRate;
    }

    public void setMinReentryProfitRate(BigDecimal minReentryProfitRate) {
        this.minReentryProfitRate = minReentryProfitRate == null ? BigDecimal.ZERO : minReentryProfitRate;
    }
}
