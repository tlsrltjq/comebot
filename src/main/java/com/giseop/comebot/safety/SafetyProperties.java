package com.giseop.comebot.safety;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "safety")
public class SafetyProperties {

    private boolean killSwitchEnabled = false;

    public boolean isKillSwitchEnabled() {
        return killSwitchEnabled;
    }

    public void setKillSwitchEnabled(boolean killSwitchEnabled) {
        this.killSwitchEnabled = killSwitchEnabled;
    }
}
