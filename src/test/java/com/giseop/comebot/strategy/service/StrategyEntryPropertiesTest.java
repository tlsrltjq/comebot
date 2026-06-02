package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyEntryPropertiesTest {

    @Test
    void emptyAllowListAllowsAllHours() {
        StrategyEntryProperties props = new StrategyEntryProperties();

        for (int h = 0; h < 24; h++) {
            assertThat(props.isTradingHourAllowed(h)).isTrue();
        }
    }

    @Test
    void onlyWhitelistedHoursAreAllowed() {
        StrategyEntryProperties props = new StrategyEntryProperties();
        props.setAllowedHoursKst(List.of(0, 12, 17, 21, 23));

        assertThat(props.isTradingHourAllowed(0)).isTrue();
        assertThat(props.isTradingHourAllowed(12)).isTrue();
        assertThat(props.isTradingHourAllowed(23)).isTrue();

        assertThat(props.isTradingHourAllowed(1)).isFalse();
        assertThat(props.isTradingHourAllowed(8)).isFalse();
        assertThat(props.isTradingHourAllowed(14)).isFalse();
    }

    @Test
    void nullAllowListResetsToEmptyAndAllowsAll() {
        StrategyEntryProperties props = new StrategyEntryProperties();
        props.setAllowedHoursKst(List.of(3));
        props.setAllowedHoursKst(null);

        assertThat(props.getAllowedHoursKst()).isEmpty();
        assertThat(props.isTradingHourAllowed(3)).isTrue();
    }

    @Test
    void setterCopiesListDefensively() {
        StrategyEntryProperties props = new StrategyEntryProperties();
        var source = new java.util.ArrayList<>(List.of(8, 9));
        props.setAllowedHoursKst(source);
        source.add(10);

        assertThat(props.isTradingHourAllowed(10)).isFalse();
    }
}
