package com.giseop.comebot.config;

import com.giseop.comebot.strategy.service.StrategyType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy")
public class StrategySelectionProperties {

    private StrategyType selected = StrategyType.SIMPLE_THRESHOLD;

    public StrategyType getSelected() {
        return selected;
    }

    public void setSelected(StrategyType selected) {
        this.selected = selected == null ? StrategyType.SIMPLE_THRESHOLD : selected;
    }

    public String getStrategyName() {
        return selected.displayName();
    }
}
