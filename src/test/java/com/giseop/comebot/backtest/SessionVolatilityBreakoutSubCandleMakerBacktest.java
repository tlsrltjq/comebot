package com.giseop.comebot.backtest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SessionVolatilityBreakoutSubCandleMakerBacktest {

    private final List<CandleSeries> signalSeries;
    private final Map<String, CandleSeries> fillSeriesByMarket;
    private final SessionVolatilityBreakoutConfig config;
    private final long limitValiditySec;

    SessionVolatilityBreakoutSubCandleMakerBacktest(
            List<CandleSeries> signalSeries,
            List<CandleSeries> fillSeries,
            SessionVolatilityBreakoutConfig config,
            long limitValiditySec
    ) {
        this.signalSeries = signalSeries;
        this.fillSeriesByMarket = byMarket(fillSeries);
        this.config = config;
        this.limitValiditySec = limitValiditySec;
    }

    BacktestEngine.Result run() {
        int[] pointer = new int[signalSeries.size()];
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
                tryExit(signalSeries.get(i), pointer[i], open, closed);
            }

            if (open.size() < config.maxOpenPositions()) {
                Candidate best = bestCandidate(active, pointer, open);
                if (best != null) {
                    signals++;
                    CandleSeries s = signalSeries.get(best.seriesIndex());
                    int idx = pointer[best.seriesIndex()];
                    Fill fill = findFill(s.market(), s.closeTimeSec(idx), s.close(idx));
                    if (fill.filled()) {
                        double quantity = config.orderAmount() / fill.price();
                        open.put(s.market(), new OpenTrade(fill.timeSec(), fill.price(), quantity, idx + 1));
                        fills++;
                    } else {
                        expiries++;
                    }
                }
            }

            for (int i : active) {
                pointer[i]++;
            }
        }

        return new BacktestEngine.Result(closed, signals, fills, expiries,
                config.trainTestSplitSec(), config.initialCapital());
    }

    private static Map<String, CandleSeries> byMarket(List<CandleSeries> series) {
        Map<String, CandleSeries> result = new HashMap<>();
        for (CandleSeries s : series) {
            result.put(s.market(), s);
        }
        return result;
    }

    private Fill findFill(String market, long signalCloseSec, double limitPrice) {
        CandleSeries fillSeries = fillSeriesByMarket.get(market);
        if (fillSeries == null) {
            return Fill.notFilled();
        }
        long validUntilSec = signalCloseSec + limitValiditySec;
        int idx = Math.max(0, fillSeries.lastClosedIndex(signalCloseSec) + 1);
        while (idx < fillSeries.size()
                && fillSeries.candleTimeSec(idx) >= signalCloseSec
                && fillSeries.closeTimeSec(idx) <= validUntilSec) {
            if (fillSeries.low(idx) <= limitPrice) {
                return new Fill(true, fillSeries.closeTimeSec(idx), limitPrice);
            }
            idx++;
        }
        return Fill.notFilled();
    }

    private long minClose(int[] pointer) {
        long minClose = Long.MAX_VALUE;
        for (int i = 0; i < signalSeries.size(); i++) {
            if (pointer[i] < signalSeries.get(i).size()) {
                minClose = Math.min(minClose, signalSeries.get(i).closeTimeSec(pointer[i]));
            }
        }
        return minClose;
    }

    private List<Integer> activeIndexes(int[] pointer, long minClose) {
        List<Integer> active = new ArrayList<>();
        for (int i = 0; i < signalSeries.size(); i++) {
            if (pointer[i] < signalSeries.get(i).size()
                    && signalSeries.get(i).closeTimeSec(pointer[i]) == minClose) {
                active.add(i);
            }
        }
        return active;
    }

    private Candidate bestCandidate(List<Integer> active, int[] pointer, Map<String, OpenTrade> open) {
        Candidate best = null;
        for (int i : active) {
            CandleSeries s = signalSeries.get(i);
            int idx = pointer[i];
            if (open.containsKey(s.market())
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

    private record Fill(boolean filled, long timeSec, double price) {
        static Fill notFilled() {
            return new Fill(false, 0, 0);
        }
    }

    private record OpenTrade(long entryTimeSec, double entryPrice, double quantity, int entryIndex) {
    }
}
