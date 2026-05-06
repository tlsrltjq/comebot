package com.giseop.comebot.mvp2.simulation;

public enum MarketType {
    SPOT_LONG(true),
    FUTURES_LONG_SIM(true),
    FUTURES_SHORT_SIM(true);

    private final boolean simulationOnly;

    MarketType(boolean simulationOnly) {
        this.simulationOnly = simulationOnly;
    }

    public boolean isSimulationOnly() {
        return simulationOnly;
    }
}
