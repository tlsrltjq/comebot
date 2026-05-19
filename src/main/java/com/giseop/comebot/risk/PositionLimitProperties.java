package com.giseop.comebot.risk;

import com.giseop.comebot.exchange.ExchangeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk.position-limit")
public class PositionLimitProperties {

    private boolean enabled = true;
    private int upbitMaxPositions = 3;
    private int binanceMaxPositions = 3;
    private int totalMaxPositions = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getUpbitMaxPositions() {
        return upbitMaxPositions;
    }

    public void setUpbitMaxPositions(int upbitMaxPositions) {
        this.upbitMaxPositions = upbitMaxPositions <= 0 ? 3 : upbitMaxPositions;
    }

    public int getBinanceMaxPositions() {
        return binanceMaxPositions;
    }

    public void setBinanceMaxPositions(int binanceMaxPositions) {
        this.binanceMaxPositions = binanceMaxPositions <= 0 ? 3 : binanceMaxPositions;
    }

    public int getTotalMaxPositions() {
        return totalMaxPositions;
    }

    public void setTotalMaxPositions(int totalMaxPositions) {
        this.totalMaxPositions = totalMaxPositions <= 0 ? 5 : totalMaxPositions;
    }

    public int exchangeMaxPositions(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? binanceMaxPositions : upbitMaxPositions;
    }
}
