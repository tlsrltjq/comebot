package com.giseop.comebot.backtest;

import java.util.List;
import java.util.Locale;

/**
 * Aggregate performance metrics over a set of {@link ClosedTrade}s. The same
 * report is produced for the full sample and for the train/test split so the
 * walk-forward (out-of-sample) gate can be read at a glance.
 */
final class BacktestReport {

    private final String label;
    private final int trades;
    private final int wins;
    private final int takeProfits;
    private final int stopLosses;
    private final double winRatePct;
    private final double profitFactor;
    private final double grossProfitFactor;
    private final double netPnl;
    private final double avgReturnPct;
    private final double avgWinPct;
    private final double avgLossPct;
    private final double avgHoldMinutes;
    private final double maxDrawdownPct;

    private BacktestReport(
            String label,
            int trades,
            int wins,
            int takeProfits,
            int stopLosses,
            double winRatePct,
            double profitFactor,
            double grossProfitFactor,
            double netPnl,
            double avgReturnPct,
            double avgWinPct,
            double avgLossPct,
            double avgHoldMinutes,
            double maxDrawdownPct
    ) {
        this.label = label;
        this.trades = trades;
        this.wins = wins;
        this.takeProfits = takeProfits;
        this.stopLosses = stopLosses;
        this.winRatePct = winRatePct;
        this.profitFactor = profitFactor;
        this.grossProfitFactor = grossProfitFactor;
        this.netPnl = netPnl;
        this.avgReturnPct = avgReturnPct;
        this.avgWinPct = avgWinPct;
        this.avgLossPct = avgLossPct;
        this.avgHoldMinutes = avgHoldMinutes;
        this.maxDrawdownPct = maxDrawdownPct;
    }

    int trades() {
        return trades;
    }

    double profitFactor() {
        return profitFactor;
    }

    static BacktestReport of(String label, List<ClosedTrade> trades, double initialCapital) {
        int n = trades.size();
        int wins = 0;
        int takeProfits = 0;
        int stopLosses = 0;
        double grossProfit = 0;
        double grossLoss = 0;
        double rawProfit = 0;
        double rawLoss = 0;
        double sumReturn = 0;
        double sumWinPct = 0;
        double sumLossPct = 0;
        long sumHold = 0;
        // Equity curve seeded with starting capital so drawdown stays a sane %.
        double equity = initialCapital;
        double peak = initialCapital;
        double maxDrawdownPct = 0;

        for (ClosedTrade trade : trades) {
            sumReturn += trade.returnPct();
            sumHold += trade.holdSeconds();
            if ("TP".equals(trade.exitReason())) {
                takeProfits++;
            } else if ("SL".equals(trade.exitReason())) {
                stopLosses++;
            }
            if (trade.isWin()) {
                wins++;
                grossProfit += trade.netPnl();
                sumWinPct += trade.returnPct();
            } else {
                grossLoss += Math.abs(trade.netPnl());
                sumLossPct += trade.returnPct();
            }
            if (trade.isGrossWin()) {
                rawProfit += trade.grossPnl();
            } else {
                rawLoss += Math.abs(trade.grossPnl());
            }
            equity += trade.netPnl();
            peak = Math.max(peak, equity);
            if (peak > 0) {
                maxDrawdownPct = Math.max(maxDrawdownPct, (peak - equity) / peak * 100.0);
            }
        }

        int losses = n - wins;
        double profitFactor = grossLoss == 0 ? (grossProfit > 0 ? Double.POSITIVE_INFINITY : 0) : grossProfit / grossLoss;
        double grossProfitFactor = rawLoss == 0 ? (rawProfit > 0 ? Double.POSITIVE_INFINITY : 0) : rawProfit / rawLoss;

        return new BacktestReport(
                label,
                n,
                wins,
                takeProfits,
                stopLosses,
                n == 0 ? 0 : wins * 100.0 / n,
                profitFactor,
                grossProfitFactor,
                grossProfit - grossLoss,
                n == 0 ? 0 : sumReturn / n,
                wins == 0 ? 0 : sumWinPct / wins,
                losses == 0 ? 0 : sumLossPct / losses,
                n == 0 ? 0 : sumHold / 60.0 / n,
                maxDrawdownPct
        );
    }

    String format() {
        return String.format(
                Locale.US,
                "%-6s | trades=%4d  win=%5.1f%%  PFnet=%6.3f  PFgross=%6.3f  netPnL=%,12.0f  TP/SL=%4d/%4d  "
                        + "avgWin=%+6.3f%%  avgLoss=%+6.3f%%  avgHold=%7.1fmin  MDD=%5.1f%%",
                label, trades, winRatePct, profitFactor, grossProfitFactor, netPnl, takeProfits, stopLosses,
                avgWinPct, avgLossPct, avgHoldMinutes, maxDrawdownPct);
    }
}
