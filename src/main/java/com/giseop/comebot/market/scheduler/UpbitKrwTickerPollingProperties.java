package com.giseop.comebot.market.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market.upbit-krw-ticker-polling")
public class UpbitKrwTickerPollingProperties {

    private boolean enabled = true;
    private boolean bootstrapOnStartup = true;
    private long fixedDelayMs = 600000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBootstrapOnStartup() {
        return bootstrapOnStartup;
    }

    public void setBootstrapOnStartup(boolean bootstrapOnStartup) {
        this.bootstrapOnStartup = bootstrapOnStartup;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs <= 0 ? 600000 : fixedDelayMs;
    }
}
