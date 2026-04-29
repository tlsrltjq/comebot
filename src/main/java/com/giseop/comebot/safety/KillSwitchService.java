package com.giseop.comebot.safety;

import org.springframework.stereotype.Service;

@Service
public class KillSwitchService {

    private final SafetyProperties safetyProperties;

    public KillSwitchService(SafetyProperties safetyProperties) {
        this.safetyProperties = safetyProperties;
    }

    public boolean isEnabled() {
        return safetyProperties.isKillSwitchEnabled();
    }
}
