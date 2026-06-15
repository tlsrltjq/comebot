package com.giseop.comebot.backtest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RankedRotationBacktest {

    private final List<CandleSeries> series;
    private final RankedRotationConfig config;

    RankedRotationBacktest(List<CandleSeries> series, RankedRotationConfig config) {
        this.series = series;
        this.config = config;
    }

    BacktestEngine.Result run() {
        int[] pointer = new int[series.size()];
        Map<String, OpenTrade> open = new HashMap<>();
        List<ClosedTrade> closed = new ArrayList<>();
        long signals = 0;
        long fills = 0;

        while (true) {
            long minClose = minClose(pointer);
            if (minClose == Long.MAX_VALUE) {
                break;
            }

            List<Integer> active = activeIndexes(pointer, minClose);
            for (int i : active) {
                tryExit(series.get(i), pointer[i], open, closed);
            }

            if (shouldRebalance(active, pointer)) {
                List<Candidate> ranked = rankedCandidates(active, pointer);
                Set<String> targetMarkets = targetMarkets(ranked);
                rotateOut(active, pointer, targetMarkets, open, closed);
                for (Candidate candidate : ranked) {
                    if (open.size() >= config.rankCount()) {
                        break;
                    }
                    CandleSeries s = series.get(candidate.seriesIndex());
                    if (open.containsKey(s.market())) {
                        continue;
                    }
                    signals++;
                    int next = pointer[candidate.seriesIndex()] + 1;
                    if (next < s.size()) {
                        double entryPrice = s.open(next);
                        double quantity = config.orderAmount() / entryPrice;
                        long entryTime = s.closeTimeSec(pointer[candidate.seriesIndex()]);
                        open.put(s.market(), new OpenTrade(entryTime, entryPrice, quantity, next));
                        fills++;
                    }
                }
            }

            for (int i : active) {
                pointer[i]++;
            }
        }

        return new BacktestEngine.Result(closed, signals, fills, 0,
                config.trainTestSplitSec(), config.initialCapital());
    }

    private long minClose(int[] pointer) {
        long minClose = Long.MAX_VALUE;
        for (int i = 0; i < series.size(); i++) {
            if (pointer[i] < series.get(i).size()) {
                minClose = Math.min(minClose, series.get(i).closeTimeSec(pointer[i]));
            }
        }
        return minClose;
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

    private boolean shouldRebalance(List<Integer> active, int[] pointer) {
        for (int i : active) {
            if (pointer[i] >= config.lookbackCandles()
                    && pointer[i] % config.rebalanceEveryCandles() == 0) {
                return true;
            }
        }
        return false;
    }

    private List<Candidate> rankedCandidates(List<Integer> active, int[] pointer) {
        return active.stream()
                .map(i -> candidate(i, pointer[i]))
                .filter(candidate -> candidate.returnPct() >= config.minReturnPct())
                .sorted(Comparator.comparing(Candidate::returnPct).reversed())
                .limit(config.rankCount())
                .toList();
    }

    private Candidate candidate(int seriesIndex, int idx) {
        CandleSeries s = series.get(seriesIndex);
        int lookbackIdx = idx - config.lookbackCandles();
        if (lookbackIdx < 0 || s.close(lookbackIdx) <= 0) {
            return new Candidate(seriesIndex, Double.NEGATIVE_INFINITY);
        }
        double returnPct = (s.close(idx) / s.close(lookbackIdx) - 1.0) * 100.0;
        return new Candidate(seriesIndex, returnPct);
    }

    private Set<String> targetMarkets(List<Candidate> ranked) {
        Set<String> target = new HashSet<>();
        for (Candidate candidate : ranked) {
            target.add(series.get(candidate.seriesIndex()).market());
        }
        return target;
    }

    private void rotateOut(
            List<Integer> active,
            int[] pointer,
            Set<String> targetMarkets,
            Map<String, OpenTrade> open,
            List<ClosedTrade> closed
    ) {
        for (int i : active) {
            CandleSeries s = series.get(i);
            OpenTrade trade = open.get(s.market());
            if (trade == null || targetMarkets.contains(s.market()) || pointer[i] <= trade.entryIndex()) {
                continue;
            }
            double grossExitPrice = s.close(pointer[i]);
            double exitFill = grossExitPrice * (1.0 - config.slippageRate());
            closed.add(buildClosedTrade(s.market(), trade, grossExitPrice, exitFill,
                    s.closeTimeSec(pointer[i]), "ROTATE"));
            open.remove(s.market());
        }
    }

    private void tryExit(CandleSeries s, int idx, Map<String, OpenTrade> open, List<ClosedTrade> closed) {
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
        return new ClosedTrade(market, trade.entryTimeSec(), exitTimeSec, trade.entryPrice(),
                exitFill, netPnl, grossPnl, returnPct, reason);
    }

    private record Candidate(int seriesIndex, double returnPct) {
    }

    private record OpenTrade(long entryTimeSec, double entryPrice, double quantity, int entryIndex) {
    }
}
