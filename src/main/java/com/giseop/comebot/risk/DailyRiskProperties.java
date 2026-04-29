package com.giseop.comebot.risk;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk")
public class DailyRiskProperties {

    private int dailyOrderLimit = 10;
    private BigDecimal dailyLossLimit = new BigDecimal("50000");
    private boolean dailyRiskEnabled = false;

    public int getDailyOrderLimit() {
        return dailyOrderLimit;
    }

    public void setDailyOrderLimit(int dailyOrderLimit) {
        this.dailyOrderLimit = dailyOrderLimit;
    }

    public BigDecimal getDailyLossLimit() {
        return dailyLossLimit;
    }

    public void setDailyLossLimit(BigDecimal dailyLossLimit) {
        this.dailyLossLimit = dailyLossLimit == null ? new BigDecimal("50000") : dailyLossLimit;
    }

    public boolean isDailyRiskEnabled() {
        return dailyRiskEnabled;
    }

    public void setDailyRiskEnabled(boolean dailyRiskEnabled) {
        this.dailyRiskEnabled = dailyRiskEnabled;
    }
}
