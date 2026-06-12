package com.giseop.comebot.backtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RelativeStrengthMomentumBacktest {

    private final List<CandleSeries> series;
    private final RelativeStrengthMomentumConfig config;

    RelativeStrengthMomentumBacktest(List<CandleSeries> series, RelativeStrengthMomentumConfig config) {
        this.series = series;
        this.config = config;
    }

    BacktestEngine.Result run() {
        int marketCount = series.size();
        int[] pointer = new int[marketCount];
        Map<String, OpenTrade> open = new HashMap<>();
        List<ClosedTrade> closed = new ArrayList<>();
        long signals = 0;
        long fills = 0;

        while (true) {
            long minClose = Long.MAX_VALUE;
            for (int i = 0; i < marketCount; i++) {
                if (pointer[i] < series.get(i).size()) {
                    minClose = Math.min(minClose, series.get(i).closeTimeSec(pointer[i]));
                }
            }
            if (minClose == Long.MAX_VALUE) {
                break;
            }

            List<Integer> active = activeIndexes(pointer, minClose);
            for (int i : active) {
                tryExit(series.get(i), pointer[i], open, closed);
            }

            if (open.size() < config.maxOpenPositions()) {
                Candidate best = bestCandidate(active, pointer, open);
                if (best != null) {
                    signals++;
                    int next = pointer[best.seriesIndex()] + 1;
                    CandleSeries s = series.get(best.seriesIndex());
                    if (next < s.size()) {
                        double entryPrice = s.open(next);
                        double quantity = config.orderAmount() / entryPrice;
                        long entryTime = s.closeTimeSec(pointer[best.seriesIndex()]);
                        open.put(s.market(), new OpenTrade(entryTime, entryPrice, quantity, next));
                        fills++;
                    }
                }
            }

            for (int i : active) {
                pointer[i]++;
            }
        }

        return new BacktestEngine.Result(
                closed,
                signals,
                fills,
                0,
                config.trainTestSplitSec(),
                config.initialCapital());
    }

    private List<Integer> activeIndexes(int[] pointer, long minClose) {
        List<Integer> active = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            if (pointer[i] < series.get(i).size() && series.get(i).closeTimeSec(pointer[i]) == minClose) {
                active.add(i);
            }
        }
        return active;
    }

    private Candidate bestCandidate(List<Integer> active, int[] pointer, Map<String, OpenTrade> open) {
        Candidate best = null;
        for (int i : active) {
            CandleSeries s = series.get(i);
            if (open.containsKey(s.market())) {
                continue;
            }
            int idx = pointer[i];
            int lookbackIdx = idx - config.lookbackCandles();
            if (lookbackIdx < 0 || s.close(lookbackIdx) <= 0) {
                continue;
            }
            double returnPct = (s.close(idx) / s.close(lookbackIdx) - 1.0) * 100.0;
            if (returnPct < config.minReturnPct()) {
                continue;
            }
            if (best == null || returnPct > best.returnPct()) {
                best = new Candidate(i, returnPct);
            }
        }
        return best;
    }

    private void tryExit(
            CandleSeries s,
            int idx,
            Map<String, OpenTrade> open,
            List<ClosedTrade> closed
    ) {
        OpenTrade trade = open.get(s.market());
        if (trade == null || idx <= trade.entryIndex()) {
            return;
        }

        double tpPrice = trade.entryPrice() * (1.0 + config.takeProfitPct() / 100.0);
        double slPrice = trade.entryPrice() * (1.0 + config.stopLossPct() / 100.0);
        boolean stopLoss = s.low(idx) <= slPrice;
        boolean takeProfit = s.high(idx) >= tpPrice;
        boolean timeout = config.maxHoldCandles() > 0 && idx - trade.entryIndex() >= config.maxHoldCandles();
        if (!stopLoss && !takeProfit && !timeout) {
            return;
        }

        String reason;
        double grossExitPrice;
        if (stopLoss) {
            reason = "SL";
            grossExitPrice = slPrice;
        } else if (takeProfit) {
            reason = "TP";
            grossExitPrice = tpPrice;
        } else {
            reason = "TIME";
            grossExitPrice = s.close(idx);
        }

        double exitFill = grossExitPrice * (1.0 - config.slippageRate());
        closed.add(buildClosedTrade(s.market(), trade, grossExitPrice, exitFill, s.closeTimeSec(idx), reason));
        open.remove(s.market());
    }

    private ClosedTrade buildClosedTrade(
            String market,
            OpenTrade trade,
            double grossExitPrice,
            double exitFill,
            long exitTimeSec,
            String reason
    ) {
        double buyAmount = trade.quantity() * trade.entryPrice();
        double feeBuy = buyAmount * config.makerFeeRate();
        double sellGross = trade.quantity() * exitFill;
        double feeSell = sellGross * config.takerFeeRate();
        double netPnl = (sellGross - feeSell) - (buyAmount + feeBuy);
        double grossPnl = trade.quantity() * (grossExitPrice - trade.entryPrice());
        double costBasis = buyAmount + feeBuy;
        double returnPct = costBasis == 0 ? 0 : netPnl / costBasis * 100.0;
        return new ClosedTrade(
                market,
                trade.entryTimeSec(),
                exitTimeSec,
                trade.entryPrice(),
                exitFill,
                netPnl,
                grossPnl,
                returnPct,
                reason);
    }

    private record Candidate(int seriesIndex, double returnPct) {
    }

    private record OpenTrade(long entryTimeSec, double entryPrice, double quantity, int entryIndex) {
    }
}
