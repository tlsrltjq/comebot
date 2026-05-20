package com.giseop.comebot.scheduler;

import com.giseop.comebot.exchange.ExchangeMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading.candidate-scheduler")
public class CandidateSchedulerProperties {

    private boolean enabled = false;
    private boolean notifySummary = false;
    private long fixedDelayMs = 60000;
    private long perMarketDelayMs = 0;
    private int maxBuysPerRun = 2;
    private ExchangeMode exchange = ExchangeMode.UPBIT;
    private List<ExchangeMode> exchanges = new ArrayList<>();
    private List<String> markets = new ArrayList<>(List.of("KRW-BTC", "KRW-ETH"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNotifySummary() {
        return notifySummary;
    }

    public void setNotifySummary(boolean notifySummary) {
        this.notifySummary = notifySummary;
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

    public int getMaxBuysPerRun() {
        return maxBuysPerRun;
    }

    public void setMaxBuysPerRun(int maxBuysPerRun) {
        this.maxBuysPerRun = Math.max(0, maxBuysPerRun);
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

    public List<String> getMarkets() {
        return markets;
    }

    public void setMarkets(List<String> markets) {
        this.markets = markets == null ? new ArrayList<>() : markets;
    }
}
