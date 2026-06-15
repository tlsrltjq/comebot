package com.giseop.comebot.backtest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SessionVolatilityBreakoutMakerBacktest {

    private final List<CandleSeries> series;
    private final SessionVolatilityBreakoutConfig config;
    private final int limitValidityCandles;

    SessionVolatilityBreakoutMakerBacktest(
            List<CandleSeries> series,
            SessionVolatilityBreakoutConfig config,
            int limitValidityCandles
    ) {
        this.series = series;
        this.config = config;
        this.limitValidityCandles = limitValidityCandles;
    }

    BacktestEngine.Result run() {
        int[] pointer = new int[series.size()];
        Map<String, PendingOrder> pending = new HashMap<>();
        Map<String, OpenTrade> open = new HashMap<>();
        List<ClosedTrade> closed = new ArrayList<>();
        long signals = 0;
        long fills = 0;
        long expiries = 0;

        while (true) {
            long minClose = minClose(pointer);
            if (minClose == Long.MAX_VALUE) {
                break;
            }

            List<Integer> active = activeIndexes(pointer, minClose);
            for (int i : active) {
                CandleSeries s = series.get(i);
                int idx = pointer[i];
                tryExit(s, idx, open, closed);
                FillResult fillResult = tryFillOrExpire(s, idx, pending, open);
                fills += fillResult.fills();
                expiries += fillResult.expiries();
            }

            if (open.size() + pending.size() < config.maxOpenPositions()) {
                Candidate best = bestCandidate(active, pointer, open, pending);
                if (best != null) {
                    signals++;
                    CandleSeries s = series.get(best.seriesIndex());
                    int idx = pointer[best.seriesIndex()];
                    pending.put(s.market(), new PendingOrder(
                            s.closeTimeSec(idx),
                            s.close(idx),
                            idx,
                            limitValidityCandles));
                }
            }

            for (int i : active) {
                pointer[i]++;
            }
        }

        return new BacktestEngine.Result(closed, signals, fills, expiries,
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

    private FillResult tryFillOrExpire(
            CandleSeries s,
            int idx,
            Map<String, PendingOrder> pending,
            Map<String, OpenTrade> open
    ) {
        PendingOrder order = pending.get(s.market());
        if (order == null) {
            return new FillResult(0, 0);
        }
        if (idx <= order.signalIndex()) {
            return new FillResult(0, 0);
        }
        if (idx - order.signalIndex() > order.validityCandles()) {
            pending.remove(s.market());
            return new FillResult(0, 1);
        }
        if (s.low(idx) <= order.limitPrice()) {
            double quantity = config.orderAmount() / order.limitPrice();
            open.put(s.market(), new OpenTrade(order.signalTimeSec(), order.limitPrice(), quantity, idx));
            pending.remove(s.market());
            return new FillResult(1, 0);
        }
        return new FillResult(0, 0);
    }

    private Candidate bestCandidate(
            List<Integer> active,
            int[] pointer,
            Map<String, OpenTrade> open,
            Map<String, PendingOrder> pending
    ) {
        Candidate best = null;
        for (int i : active) {
            CandleSeries s = series.get(i);
            int idx = pointer[i];
            if (open.containsKey(s.market())
                    || pending.containsKey(s.market())
                    || idx - config.breakoutWindowCandles() < 0
                    || idx - config.averageWindowCandles() < 0
                    || s.open(idx) <= 0) {
                continue;
            }
            if (!inSession(s.closeTimeSec(idx))) {
                continue;
            }

            double priorHigh = highestHigh(s, idx - config.breakoutWindowCandles(), idx);
            if (s.close(idx) <= priorHigh) {
                continue;
            }
            double avgRangePct = averageRangePct(s, idx - config.averageWindowCandles(), idx);
            if (avgRangePct <= 0) {
                continue;
            }
            double rangeRatio = s.highLowRangePct(idx) / avgRangePct;
            if (rangeRatio < config.minRangeRatio()) {
                continue;
            }
            double avgAmount = averageAmount(s, idx - config.averageWindowCandles(), idx);
            if (avgAmount <= 0) {
                continue;
            }
            double volumeRatio = s.accTradePrice(idx) / avgAmount;
            if (volumeRatio < config.minVolumeRatio()) {
                continue;
            }
            double closeLocationPct = closeLocationPct(s, idx);
            if (closeLocationPct < config.minCloseLocationPct()) {
                continue;
            }

            double breakoutPct = (s.close(idx) / priorHigh - 1.0) * 100.0;
            double score = breakoutPct * rangeRatio * volumeRatio * Math.max(closeLocationPct, 1.0);
            if (best == null || score > best.score()) {
                best = new Candidate(i, score);
            }
        }
        return best;
    }

    private boolean inSession(long closeTimeSec) {
        int hour = Instant.ofEpochSecond(closeTimeSec).atZone(ZoneOffset.UTC).getHour();
        int start = config.sessionStartHourUtc();
        int end = config.sessionEndHourUtc();
        if (start == end) {
            return true;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        return hour >= start || hour < end;
    }

    private double highestHigh(CandleSeries s, int fromInclusive, int toExclusive) {
        double high = Double.NEGATIVE_INFINITY;
        for (int i = fromInclusive; i < toExclusive; i++) {
            high = Math.max(high, s.high(i));
        }
        return high;
    }

    private double averageRangePct(CandleSeries s, int fromInclusive, int toExclusive) {
        double sum = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            sum += s.highLowRangePct(i);
        }
        return sum / (toExclusive - fromInclusive);
    }

    private double averageAmount(CandleSeries s, int fromInclusive, int toExclusive) {
        double sum = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            sum += s.accTradePrice(i);
        }
        return sum / (toExclusive - fromInclusive);
    }

    private double closeLocationPct(CandleSeries s, int idx) {
        double range = s.high(idx) - s.low(idx);
        if (range <= 0) {
            return 0;
        }
        return (s.close(idx) - s.low(idx)) / range * 100.0;
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

    private record Candidate(int seriesIndex, double score) {
    }

    private record PendingOrder(long signalTimeSec, double limitPrice, int signalIndex, int validityCandles) {
    }

    private record OpenTrade(long entryTimeSec, double entryPrice, double quantity, int entryIndex) {
    }

    private record FillResult(long fills, long expiries) {
    }
}
