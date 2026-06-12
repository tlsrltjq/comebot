package com.giseop.comebot.backtest;

record RelativeStrengthMomentumConfig(
        int lookbackCandles,
        double minReturnPct,
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
