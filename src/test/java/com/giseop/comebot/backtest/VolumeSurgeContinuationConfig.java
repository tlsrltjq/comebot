package com.giseop.comebot.backtest;

record VolumeSurgeContinuationConfig(
        int averageWindowCandles,
        double minVolumeRatio,
        double minCandleReturnPct,
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
