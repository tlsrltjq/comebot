package com.giseop.comebot.backtest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes market-regime features at a given timestamp from cached candles, for
 * decomposing V1 trades and driving regime {@link EntryGate}s. All features are
 * explainable and derived only from the on-disk cache (no look-ahead):
 *
 * <ul>
 *   <li>BTC 1h trend — EMA5 vs EMA10 on the last 20 hourly closes (ops semantics)</li>
 *   <li>BTC recent return — % change over the last N hourly candles</li>
 *   <li>BTC volatility — stdev of the last N hourly returns (%)</li>
 *   <li>market breadth — fraction of tracked markets up over the last hour
 *       (a backtest-able proxy for the live-only sentiment score)</li>
 * </ul>
 */
final class RegimeContext {

    enum Trend { UP, NEUTRAL, DOWN }

    private static final int TREND_WINDOW = 20;
    private static final int EMA_SHORT = 5;
    private static final int EMA_LONG = 10;
    private static final long HOUR_SEC = 3600L;

    private final CandleSeries btc;
    private final List<CandleSeries> markets;
    private final Map<String, CandleSeries> marketByName = new HashMap<>();

    RegimeContext(CandleSeries btcHourly, List<CandleSeries> markets) {
        this.btc = btcHourly;
        this.markets = markets;
        for (CandleSeries s : markets) {
            marketByName.put(s.market(), s);
        }
    }

    /**
     * Higher-timeframe trend of the traded coin itself: EMA5 vs EMA10 over the last
     * 20 hourly-sampled closes of {@code market} (a multi-timeframe confirmation of the
     * 1m pullback — buying a dip inside a higher-TF uptrend vs a falling knife).
     */
    Trend coinHourlyTrend(String market, long timeSec) {
        CandleSeries s = marketByName.get(market);
        if (s == null) {
            return Trend.NEUTRAL;
        }
        double[] closes = new double[TREND_WINDOW];
        int n = 0;
        for (int k = TREND_WINDOW - 1; k >= 0; k--) { // oldest first
            int idx = s.lastClosedIndex(timeSec - (long) k * HOUR_SEC);
            if (idx >= 0) {
                closes[n++] = s.close(idx);
            }
        }
        if (n < EMA_LONG) {
            return Trend.NEUTRAL;
        }
        double emaShort = emaArray(closes, n, EMA_SHORT);
        double emaLong = emaArray(closes, n, EMA_LONG);
        if (emaShort > emaLong) {
            return Trend.UP;
        }
        if (emaShort < emaLong) {
            return Trend.DOWN;
        }
        return Trend.NEUTRAL;
    }

    /** % change of the coin's own close over the last {@code minutes}, NaN if unavailable. */
    double coinReturnPct(String market, long timeSec, int minutes) {
        CandleSeries s = marketByName.get(market);
        if (s == null) {
            return Double.NaN;
        }
        int now = s.lastClosedIndex(timeSec);
        int prev = s.lastClosedIndex(timeSec - (long) minutes * 60L);
        if (now < 0 || prev < 0 || now == prev) {
            return Double.NaN;
        }
        double then = s.close(prev);
        return then == 0 ? Double.NaN : (s.close(now) - then) / then * 100.0;
    }

    Trend btcTrend(long timeSec) {
        int idx = btc.lastClosedIndex(timeSec);
        if (idx < EMA_LONG) {
            return Trend.NEUTRAL;
        }
        int from = Math.max(0, idx - TREND_WINDOW + 1);
        double emaShort = ema(from, idx, EMA_SHORT);
        double emaLong = ema(from, idx, EMA_LONG);
        if (emaShort > emaLong) {
            return Trend.UP;
        }
        if (emaShort < emaLong) {
            return Trend.DOWN;
        }
        return Trend.NEUTRAL;
    }

    /** % change of BTC close over the last {@code hours} hourly candles, NaN if unavailable. */
    double btcReturnPct(long timeSec, int hours) {
        int idx = btc.lastClosedIndex(timeSec);
        int prev = idx - hours;
        if (idx < 0 || prev < 0) {
            return Double.NaN;
        }
        double now = btc.close(idx);
        double then = btc.close(prev);
        return then == 0 ? Double.NaN : (now - then) / then * 100.0;
    }

    /** Stdev of the last {@code hours} BTC hourly returns (%), NaN if unavailable. */
    double btcVolatilityPct(long timeSec, int hours) {
        int idx = btc.lastClosedIndex(timeSec);
        if (idx < hours) {
            return Double.NaN;
        }
        double mean = 0;
        for (int i = idx - hours + 1; i <= idx; i++) {
            mean += hourlyReturn(i);
        }
        mean /= hours;
        double var = 0;
        for (int i = idx - hours + 1; i <= idx; i++) {
            double d = hourlyReturn(i) - mean;
            var += d * d;
        }
        return Math.sqrt(var / hours);
    }

    /** Fraction of tracked markets up over the last hour at {@code timeSec}, NaN if none. */
    double breadthUp(long timeSec) {
        int up = 0;
        int considered = 0;
        for (CandleSeries s : markets) {
            int now = s.lastClosedIndex(timeSec);
            int prev = s.lastClosedIndex(timeSec - HOUR_SEC);
            if (now < 0 || prev < 0 || now == prev) {
                continue;
            }
            considered++;
            if (s.close(now) > s.close(prev)) {
                up++;
            }
        }
        return considered == 0 ? Double.NaN : (double) up / considered;
    }

    private double hourlyReturn(int i) {
        if (i <= 0) {
            return 0;
        }
        double prev = btc.close(i - 1);
        return prev == 0 ? 0 : (btc.close(i) - prev) / prev * 100.0;
    }

    private double ema(int from, int to, int period) {
        double k = 2.0 / (period + 1);
        double e = btc.close(from);
        for (int i = from + 1; i <= to; i++) {
            e = btc.close(i) * k + e * (1 - k);
        }
        return e;
    }

    private static double emaArray(double[] vals, int n, int period) {
        double k = 2.0 / (period + 1);
        double e = vals[0];
        for (int i = 1; i < n; i++) {
            e = vals[i] * k + e * (1 - k);
        }
        return e;
    }
}
