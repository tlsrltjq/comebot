package com.giseop.comebot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.strategy.service.StrategyType;
import org.junit.jupiter.api.Test;

class StrategySelectionPropertiesTest {

    @Test
    void defaultStrategyIsSimpleThreshold() {
        StrategySelectionProperties properties = new StrategySelectionProperties();

        assertThat(properties.getSelected()).isEqualTo(StrategyType.SIMPLE_THRESHOLD);
        assertThat(properties.getStrategyName()).isEqualTo("SimpleThresholdStrategy");
    }

    @Test
    void nullStrategyFallsBackToSimpleThreshold() {
        StrategySelectionProperties properties = new StrategySelectionProperties();

        properties.setSelected(null);

        assertThat(properties.getSelected()).isEqualTo(StrategyType.SIMPLE_THRESHOLD);
    }

    @Test
    void volatilityBreakoutLongNameIsExposed() {
        StrategySelectionProperties properties = new StrategySelectionProperties();

        properties.setSelected(StrategyType.VOLATILITY_BREAKOUT_LONG);

        assertThat(properties.getStrategyName()).isEqualTo("VolatilityBreakoutLongStrategy");
    }
}
