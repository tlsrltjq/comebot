package com.giseop.comebot.backtest;

record OversoldMeanReversionConfig(
        int lookbackCandles,
        double minDropPct,
        double minDeviationFromAveragePct,
        double maxCurrentRangePct,
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
