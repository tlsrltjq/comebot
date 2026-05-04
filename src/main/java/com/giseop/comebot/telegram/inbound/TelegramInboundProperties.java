package com.giseop.comebot.telegram.inbound;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.inbound")
public class TelegramInboundProperties {

    private boolean enabled = false;
    private long fixedDelayMs = 3000;
    private boolean manualPaperExecutionEnabled = false;

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

    public boolean isManualPaperExecutionEnabled() {
        return manualPaperExecutionEnabled;
    }

    public void setManualPaperExecutionEnabled(boolean manualPaperExecutionEnabled) {
        this.manualPaperExecutionEnabled = manualPaperExecutionEnabled;
    }
}
