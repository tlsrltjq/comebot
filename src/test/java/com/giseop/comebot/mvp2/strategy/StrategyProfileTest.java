package com.giseop.comebot.mvp2.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrategyProfileTest {

    @Test
    void mvp2UsesThreeInitialStrategyProfiles() {
        assertThat(StrategyProfile.values())
                .containsExactly(StrategyProfile.STABLE, StrategyProfile.AGGRESSIVE, StrategyProfile.DEFENSIVE);
    }

    @Test
    void profileDisplayNamesAreBilingual() {
        assertThat(StrategyProfile.STABLE.getDisplayName()).isEqualTo("안정형(Stable)");
        assertThat(StrategyProfile.AGGRESSIVE.getDisplayName()).isEqualTo("공격형(Aggressive)");
        assertThat(StrategyProfile.DEFENSIVE.getDisplayName()).isEqualTo("수비형(Defensive)");
    }
}
