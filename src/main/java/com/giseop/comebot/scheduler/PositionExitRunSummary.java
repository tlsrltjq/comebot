package com.giseop.comebot.scheduler;

public record PositionExitRunSummary(
        int positionMarkets,
        int evaluatedMarkets,
        int soldCount,
        int rejectedCount,
        int holdCount,
        int failedCount
) {

    public static PositionExitRunSummary empty() {
        return new PositionExitRunSummary(0, 0, 0, 0, 0, 0);
    }

    public PositionExitRunSummary add(PositionExitRunSummary other) {
        if (other == null) {
            return this;
        }
        return new PositionExitRunSummary(
                positionMarkets + other.positionMarkets,
                evaluatedMarkets + other.evaluatedMarkets,
                soldCount + other.soldCount,
                rejectedCount + other.rejectedCount,
                holdCount + other.holdCount,
                failedCount + other.failedCount
        );
    }
}
