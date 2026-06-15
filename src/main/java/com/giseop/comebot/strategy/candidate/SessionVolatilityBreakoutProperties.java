package com.giseop.comebot.strategy.candidate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.session-volatility-breakout")
public class SessionVolatilityBreakoutProperties {

    private int signalUnitMinutes = 15;
    private int candleCount = 70;
    private int breakoutWindow = 20;
    private int averageWindow = 60;
    private int sessionStartHourUtc = 6;
    private int sessionEndHourUtc = 12;
    private BigDecimal minRangeRatio = new BigDecimal("2.5");
    private BigDecimal minVolumeRatio = new BigDecimal("1.5");
    private BigDecimal minCloseLocation = new BigDecimal("70.0");
    private List<String> excludedMarkets = new ArrayList<>(List.of(
            "USD1USDT",
            "USDCUSDT",
            "USDEUSDT",
            "XAUTUSDT"
    ));

    public int getSignalUnitMinutes() {
        return signalUnitMinutes;
    }

    public void setSignalUnitMinutes(int signalUnitMinutes) {
        this.signalUnitMinutes = Math.max(1, signalUnitMinutes);
    }

    public int getCandleCount() {
        return Math.max(candleCount, averageWindow + 2);
    }

    public void setCandleCount(int candleCount) {
        this.candleCount = Math.max(3, candleCount);
    }

    public int getBreakoutWindow() {
        return breakoutWindow;
    }

    public void setBreakoutWindow(int breakoutWindow) {
        this.breakoutWindow = Math.max(1, breakoutWindow);
    }

    public int getAverageWindow() {
        return averageWindow;
    }

    public void setAverageWindow(int averageWindow) {
        this.averageWindow = Math.max(1, averageWindow);
    }

    public int getSessionStartHourUtc() {
        return sessionStartHourUtc;
    }

    public void setSessionStartHourUtc(int sessionStartHourUtc) {
        this.sessionStartHourUtc = clampHour(sessionStartHourUtc);
    }

    public int getSessionEndHourUtc() {
        return sessionEndHourUtc;
    }

    public void setSessionEndHourUtc(int sessionEndHourUtc) {
        this.sessionEndHourUtc = clampHour(sessionEndHourUtc);
    }

    public BigDecimal getMinRangeRatio() {
        return minRangeRatio;
    }

    public void setMinRangeRatio(BigDecimal minRangeRatio) {
        this.minRangeRatio = minRangeRatio == null ? new BigDecimal("2.5") : minRangeRatio;
    }

    public BigDecimal getMinVolumeRatio() {
        return minVolumeRatio;
    }

    public void setMinVolumeRatio(BigDecimal minVolumeRatio) {
        this.minVolumeRatio = minVolumeRatio == null ? new BigDecimal("1.5") : minVolumeRatio;
    }

    public BigDecimal getMinCloseLocation() {
        return minCloseLocation;
    }

    public void setMinCloseLocation(BigDecimal minCloseLocation) {
        this.minCloseLocation = minCloseLocation == null ? new BigDecimal("70.0") : minCloseLocation;
    }

    public List<String> getExcludedMarkets() {
        return excludedMarkets;
    }

    public void setExcludedMarkets(List<String> excludedMarkets) {
        this.excludedMarkets = excludedMarkets == null ? new ArrayList<>() : new ArrayList<>(excludedMarkets);
    }

    private int clampHour(int hour) {
        if (hour < 0) {
            return 0;
        }
        if (hour > 24) {
            return 24;
        }
        return hour;
    }
}
