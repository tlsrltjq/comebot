package com.giseop.comebot.mvp2.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarketTypeTest {

    @Test
    void allMvp2MarketTypesAreSimulationOnly() {
        assertThat(MarketType.values())
                .extracting(MarketType::isSimulationOnly)
                .containsOnly(true);
    }

    @Test
    void mvp2MarketTypesSeparateSpotAndFuturesSimulation() {
        assertThat(MarketType.values())
                .containsExactly(MarketType.SPOT_LONG, MarketType.FUTURES_LONG_SIM, MarketType.FUTURES_SHORT_SIM);
    }
}
