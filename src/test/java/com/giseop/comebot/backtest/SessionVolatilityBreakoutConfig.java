package com.giseop.comebot.backtest;

record SessionVolatilityBreakoutConfig(
        int breakoutWindowCandles,
        int averageWindowCandles,
        double minRangeRatio,
        double minVolumeRatio,
        double minCloseLocationPct,
        int sessionStartHourUtc,
        int sessionEndHourUtc,
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
