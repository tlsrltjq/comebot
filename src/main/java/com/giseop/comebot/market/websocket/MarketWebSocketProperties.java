package com.giseop.comebot.market.websocket;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market.websocket")
public class MarketWebSocketProperties {

    private boolean enabled = false;
    private ExchangeToggle upbit = new ExchangeToggle();
    private ExchangeToggle binance = new ExchangeToggle();
    private long snapshotStaleMs = 5000;
    private long orderStaleMs = 3000;
    private long reconnectInitialDelayMs = 1000;
    private long reconnectMaxDelayMs = 30000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUpbitEnabled() {
        return upbit.isEnabled();
    }

    public void setUpbitEnabled(boolean upbitEnabled) {
        this.upbit.setEnabled(upbitEnabled);
    }

    public boolean isBinanceEnabled() {
        return binance.isEnabled();
    }

    public void setBinanceEnabled(boolean binanceEnabled) {
        this.binance.setEnabled(binanceEnabled);
    }

    public ExchangeToggle getUpbit() {
        return upbit;
    }

    public void setUpbit(ExchangeToggle upbit) {
        this.upbit = upbit == null ? new ExchangeToggle() : upbit;
    }

    public ExchangeToggle getBinance() {
        return binance;
    }

    public void setBinance(ExchangeToggle binance) {
        this.binance = binance == null ? new ExchangeToggle() : binance;
    }

    public long getSnapshotStaleMs() {
        return snapshotStaleMs;
    }

    public void setSnapshotStaleMs(long snapshotStaleMs) {
        this.snapshotStaleMs = positiveOrDefault(snapshotStaleMs, 5000);
    }

    public long getOrderStaleMs() {
        return orderStaleMs;
    }

    public void setOrderStaleMs(long orderStaleMs) {
        this.orderStaleMs = positiveOrDefault(orderStaleMs, 3000);
    }

    public long getReconnectInitialDelayMs() {
        return reconnectInitialDelayMs;
    }

    public void setReconnectInitialDelayMs(long reconnectInitialDelayMs) {
        this.reconnectInitialDelayMs = positiveOrDefault(reconnectInitialDelayMs, 1000);
    }

    public long getReconnectMaxDelayMs() {
        return reconnectMaxDelayMs;
    }

    public void setReconnectMaxDelayMs(long reconnectMaxDelayMs) {
        this.reconnectMaxDelayMs = positiveOrDefault(reconnectMaxDelayMs, 30000);
    }

    public Duration snapshotStaleDuration() {
        return Duration.ofMillis(snapshotStaleMs);
    }

    public Duration orderStaleDuration() {
        return Duration.ofMillis(orderStaleMs);
    }

    private long positiveOrDefault(long value, long defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    public static class ExchangeToggle {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
