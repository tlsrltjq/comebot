package com.giseop.comebot.strategy.service;

public enum StrategyType {
    SIMPLE_THRESHOLD("SimpleThresholdStrategy"),
    VOLATILITY_BREAKOUT_LONG("VolatilityBreakoutLongStrategy");

    private final String displayName;

    StrategyType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
