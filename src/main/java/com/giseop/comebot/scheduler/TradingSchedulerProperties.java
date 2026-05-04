package com.giseop.comebot.scheduler;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading.scheduler")
public class TradingSchedulerProperties {

    private boolean enabled = false;
    private long fixedDelayMs = 60000;
    private long perMarketDelayMs = 0;
    private List<String> markets = new ArrayList<>(List.of("KRW-BTC", "KRW-ETH"));

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
        this.fixedDelayMs = fixedDelayMs;
    }

    public long getPerMarketDelayMs() {
        return perMarketDelayMs;
    }

    public void setPerMarketDelayMs(long perMarketDelayMs) {
        this.perMarketDelayMs = Math.max(0, perMarketDelayMs);
    }

    public List<String> getMarkets() {
        return markets;
    }

    public void setMarkets(List<String> markets) {
        this.markets = markets == null ? new ArrayList<>() : markets;
    }
}
