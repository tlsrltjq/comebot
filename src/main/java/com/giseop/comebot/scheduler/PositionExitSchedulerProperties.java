package com.giseop.comebot.scheduler;

import com.giseop.comebot.exchange.ExchangeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading.exit-scheduler")
public class PositionExitSchedulerProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 5000;
    private long perMarketDelayMs = 0;
    private boolean saveHoldHistory = false;
    private ExchangeMode exchange = ExchangeMode.UPBIT;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs > 0 ? fixedDelayMs : 5000;
    }

    public long getPerMarketDelayMs() {
        return perMarketDelayMs;
    }

    public void setPerMarketDelayMs(long perMarketDelayMs) {
        this.perMarketDelayMs = Math.max(0, perMarketDelayMs);
    }

    public boolean isSaveHoldHistory() {
        return saveHoldHistory;
    }

    public void setSaveHoldHistory(boolean saveHoldHistory) {
        this.saveHoldHistory = saveHoldHistory;
    }

    public ExchangeMode getExchange() {
        return exchange;
    }

    public void setExchange(ExchangeMode exchange) {
        this.exchange = exchange == null ? ExchangeMode.UPBIT : exchange;
    }
}
