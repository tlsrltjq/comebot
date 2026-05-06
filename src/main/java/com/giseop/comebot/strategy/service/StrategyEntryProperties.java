package com.giseop.comebot.strategy.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.entry")
public class StrategyEntryProperties {

    private boolean preventReentryWithPosition = false;

    public boolean isPreventReentryWithPosition() {
        return preventReentryWithPosition;
    }

    public void setPreventReentryWithPosition(boolean preventReentryWithPosition) {
        this.preventReentryWithPosition = preventReentryWithPosition;
    }
}
