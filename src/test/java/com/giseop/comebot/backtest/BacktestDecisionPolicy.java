package com.giseop.comebot.backtest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared strategy candidate gate. This keeps every experiment from inventing its
 * own pass/fail language after looking at the numbers.
 */
final class BacktestDecisionPolicy {

    static final double MIN_GROSS_EDGE = 1.10;
    static final double STRONG_GROSS_EDGE = 1.15;
    static final double MAX_TRAIN_TEST_GROSS_GAP = 0.25;
    static final double MAX_TOP_MARKET_TRADE_PCT = 60.0;
    static final int MIN_FULL_TRADES = 30;
    static final int MIN_TEST_TRADES = 10;

    private BacktestDecisionPolicy() {
    }

    static String decide(BacktestEngine.Result result) {
        BacktestReport full = result.full();
        BacktestReport train = result.train();
        BacktestReport test = result.test();

        if (full.trades() < MIN_FULL_TRADES || test.trades() < MIN_TEST_TRADES) {
            return "reject:sample-too-small";
        }
        if (train.grossProfitFactor() < 1.0) {
            return "reject:no-train-gross-edge";
        }
        if (train.grossProfitFactor() < MIN_GROSS_EDGE) {
            return "reject:weak-train-gross-edge";
        }
        if (topMarketTradePct(result.closed()) > MAX_TOP_MARKET_TRADE_PCT) {
            return "reject:market-concentration";
        }
        if (test.grossProfitFactor() < 1.0
                || train.grossProfitFactor() - test.grossProfitFactor() > MAX_TRAIN_TEST_GROSS_GAP) {
            return "reject:oos-collapse";
        }
        if (test.profitFactor() < 1.0) {
            return train.grossProfitFactor() >= STRONG_GROSS_EDGE
                    ? "watch:strong-gross-net-weak"
                    : "watch:gross-edge-net-weak";
        }
        return train.grossProfitFactor() >= STRONG_GROSS_EDGE
                ? "candidate:paper-observation"
                : "candidate:weak-paper-observation";
    }

    private static double topMarketTradePct(List<ClosedTrade> trades) {
        if (trades.isEmpty()) {
            return 0;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ClosedTrade trade : trades) {
            counts.merge(trade.market(), 1, Integer::sum);
        }
        int topCount = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return topCount * 100.0 / trades.size();
    }
}
