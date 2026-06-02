package com.giseop.comebot.strategy.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.entry")
public class StrategyEntryProperties {

    private boolean preventReentryWithPosition = false;
    private BigDecimal minReentryProfitRate = BigDecimal.ZERO;
    // Allowed KST hours (0-23) for new entries. Empty = all hours allowed.
    private List<Integer> allowedHoursKst = new ArrayList<>();

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

    public List<Integer> getAllowedHoursKst() {
        return allowedHoursKst;
    }

    public void setAllowedHoursKst(List<Integer> allowedHoursKst) {
        this.allowedHoursKst = allowedHoursKst == null ? new ArrayList<>() : new ArrayList<>(allowedHoursKst);
    }

    /**
     * Returns true if a new entry is allowed at the given KST hour.
     * When the allow-list is empty the filter is disabled (all hours allowed).
     */
    public boolean isTradingHourAllowed(int hourKst) {
        return allowedHoursKst.isEmpty() || allowedHoursKst.contains(hourKst);
    }
}
