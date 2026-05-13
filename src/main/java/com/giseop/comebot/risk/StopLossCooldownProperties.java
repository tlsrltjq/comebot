package com.giseop.comebot.risk;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "risk.stop-loss-cooldown")
public class StopLossCooldownProperties {

    private boolean enabled = false;
    private Duration window = Duration.ofDays(7);
    private int triggerCount = 2;
    private Duration duration = Duration.ofHours(24);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window == null || window.isZero() || window.isNegative() ? Duration.ofDays(7) : window;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount <= 0 ? 2 : triggerCount;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration == null || duration.isZero() || duration.isNegative() ? Duration.ofHours(24) : duration;
    }
}
