package com.giseop.comebot.backtest;

record BtcMarketRegimeMomentumConfig(
        int btcLookbackCandles,
        double minBtcReturnPct,
        int marketLookbackCandles,
        double minMarketReturnPct,
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
