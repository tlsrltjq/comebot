package com.giseop.comebot.backtest;

import java.util.ArrayList;
import java.util.List;

final class StockOpeningRangeBreakoutBacktest {

    private final CandleSeries series;
    private final Config config;

    StockOpeningRangeBreakoutBacktest(CandleSeries series, Config config) {
        this.series = series;
        this.config = config;
    }

    List<ClosedTrade> run() {
        if (series.size() <= config.openingRangeBars()) {
            return List.of();
        }
        List<ClosedTrade> trades = new ArrayList<>();
        int index = config.openingRangeBars();
        while (index < series.size()) {
            double openingHigh = highestHigh(index - config.openingRangeBars(), index);
            if (series.close(index) <= openingHigh) {
                index++;
                continue;
            }
            int exitIndex = exitIndex(index);
            trades.add(closeTrade(index, exitIndex));
            index = exitIndex + 1;
        }
        return List.copyOf(trades);
    }

    private int exitIndex(int entryIndex) {
        double entryPrice = series.close(entryIndex);
        double takeProfit = entryPrice * (1.0 + config.takeProfitPct() / 100.0);
        double stopLoss = entryPrice * (1.0 - config.stopLossPct() / 100.0);
        int maxExit = Math.min(series.size() - 1, entryIndex + config.maxHoldBars());
        for (int i = entryIndex + 1; i <= maxExit; i++) {
            if (series.low(i) <= stopLoss || series.high(i) >= takeProfit) {
                return i;
            }
        }
        return maxExit;
    }

    private ClosedTrade closeTrade(int entryIndex, int exitIndex) {
        double entryPrice = series.close(entryIndex);
        double takeProfit = entryPrice * (1.0 + config.takeProfitPct() / 100.0);
        double stopLoss = entryPrice * (1.0 - config.stopLossPct() / 100.0);
        double exitPrice = series.close(exitIndex);
        String exitReason = "TIME";
        if (series.low(exitIndex) <= stopLoss) {
            exitPrice = stopLoss;
            exitReason = "SL";
        } else if (series.high(exitIndex) >= takeProfit) {
            exitPrice = takeProfit;
            exitReason = "TP";
        }

        double grossReturnPct = (exitPrice - entryPrice) / entryPrice * 100.0;
        double roundTripCostPct = config.costBps() / 100.0 * 2.0;
        double netReturnPct = grossReturnPct - roundTripCostPct;
        double capital = config.tradeCapital();
        double grossPnl = capital * grossReturnPct / 100.0;
        double netPnl = capital * netReturnPct / 100.0;
        return new ClosedTrade(
                series.market(),
                series.candleTimeSec(entryIndex),
                series.closeTimeSec(exitIndex),
                entryPrice,
                exitPrice,
                netPnl,
                grossPnl,
                netReturnPct,
                exitReason
        );
    }

    private double highestHigh(int fromInclusive, int toExclusive) {
        double result = Double.NEGATIVE_INFINITY;
        for (int i = fromInclusive; i < toExclusive; i++) {
            result = Math.max(result, series.high(i));
        }
        return result;
    }

    record Config(
            int openingRangeBars,
            int maxHoldBars,
            double takeProfitPct,
            double stopLossPct,
            double costBps,
            double tradeCapital
    ) {
        Config {
            if (openingRangeBars <= 0) {
                throw new IllegalArgumentException("openingRangeBars must be positive");
            }
            if (maxHoldBars <= 0) {
                throw new IllegalArgumentException("maxHoldBars must be positive");
            }
            if (takeProfitPct <= 0 || stopLossPct <= 0) {
                throw new IllegalArgumentException("profit and loss thresholds must be positive");
            }
            if (costBps < 0) {
                throw new IllegalArgumentException("costBps must be zero or positive");
            }
            if (tradeCapital <= 0) {
                throw new IllegalArgumentException("tradeCapital must be positive");
            }
        }
    }
}
