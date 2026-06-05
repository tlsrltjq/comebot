package com.giseop.comebot.backtest;

/**
 * One round-trip BUY→SELL trade produced by the backtest engine, fee/slippage
 * adjusted. {@code returnPct} and {@code netPnl} are net of costs so they reflect
 * the figure that would decide real-cash readiness.
 */
record ClosedTrade(
        String market,
        long entryTimeSec,
        long exitTimeSec,
        double entryPrice,
        double exitPrice,
        double netPnl,
        double grossPnl,
        double returnPct,
        String exitReason
) {

    long holdSeconds() {
        return exitTimeSec - entryTimeSec;
    }

    boolean isWin() {
        return netPnl > 0;
    }

    boolean isGrossWin() {
        return grossPnl > 0;
    }
}
