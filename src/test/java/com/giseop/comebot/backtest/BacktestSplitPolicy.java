package com.giseop.comebot.backtest;

/**
 * Single source of truth for the default out-of-sample split used by strategy
 * research tests.
 */
final class BacktestSplitPolicy {

    static final int TEST_WINDOW_DAYS = 60;
    private static final long SECONDS_PER_DAY = 86_400L;

    private BacktestSplitPolicy() {
    }

    static long splitSec(long globalEndSec) {
        return globalEndSec - (long) TEST_WINDOW_DAYS * SECONDS_PER_DAY;
    }

    static String description() {
        return "time-ordered single OOS: train=sample start..last 60d, test=last 60d";
    }
}
