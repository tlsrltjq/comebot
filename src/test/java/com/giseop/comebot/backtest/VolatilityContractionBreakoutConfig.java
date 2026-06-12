package com.giseop.comebot.backtest;

record VolatilityContractionBreakoutConfig(
        int contractionWindowCandles,
        double maxAverageRangePct,
        int breakoutWindowCandles,
        double minBreakoutPct,
        double takeProfitPct,
        double stopLossPct,
        int maxHoldCandles,
        int maxOpenPositions,
        double orderAmount,
        double makerFeeRate,
        double takerFeeRate,
        double slippageRate,
        long trainTestSplitSec,
        double initialCapital
) {
}
