package com.giseop.comebot.backtest;

record RankedRotationConfig(
        int lookbackCandles,
        double minReturnPct,
        int rankCount,
        int rebalanceEveryCandles,
        double takeProfitPct,
        double stopLossPct,
        int maxHoldCandles,
        double orderAmount,
        double makerFeeRate,
        double takerFeeRate,
        double slippageRate,
        long trainTestSplitSec,
        double initialCapital
) {
}
