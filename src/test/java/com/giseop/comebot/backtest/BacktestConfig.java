package com.giseop.comebot.backtest;

/**
 * Execution-side knobs for {@link BacktestEngine}. Strategy/exit thresholds are
 * NOT here — those live in the real {@code CandidateScannerProperties} /
 * {@code PositionExitProperties} so the production code reads them unchanged.
 */
record BacktestConfig(
        int maxOpenPositions,
        int maxBuysPerMinute,
        int limitValidityMinutes,
        double makerFeeRate,
        double takerFeeRate,
        double slippageRate,
        long trainTestSplitSec,
        double initialCapital,
        boolean intrabarOptimistic,
        boolean marketEntry
) {
}
