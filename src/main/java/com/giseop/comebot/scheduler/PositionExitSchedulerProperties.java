package com.giseop.comebot.scheduler;

import com.giseop.comebot.exchange.ExchangeMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
    private List<ExchangeMode> exchanges = new ArrayList<>();

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

    public List<ExchangeMode> getExchanges() {
        if (exchanges == null || exchanges.isEmpty()) {
            return List.of(exchange);
        }
        LinkedHashSet<ExchangeMode> normalized = new LinkedHashSet<>();
        for (ExchangeMode mode : exchanges) {
            if (mode != null) {
                normalized.add(mode);
            }
        }
        return normalized.isEmpty() ? List.of(exchange) : List.copyOf(normalized);
    }

    public void setExchanges(List<ExchangeMode> exchanges) {
        this.exchanges = exchanges == null ? new ArrayList<>() : new ArrayList<>(exchanges);
    }
}
