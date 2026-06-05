package com.giseop.comebot.backtest;

import com.giseop.comebot.market.candle.domain.Candle;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.json.JsonMapper;

/**
 * Columnar, append-only store of one market/unit candle series loaded from the
 * on-disk {@code .backtest_cache} JSON files produced by {@code backtest.py}.
 *
 * <p>Primitive arrays keep ~260k candles/market under ~15 MB so the full 7-market
 * 180-day window fits comfortably in a test JVM. {@link #windowEndingAt} rebuilds
 * the small ({@code count}-length) {@link Candle} list the operating scanner expects,
 * so the production strategy code is reused verbatim against historical data.
 */
final class CandleSeries {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String market;
    private final int unitMinutes;
    private final long[] candleTimeSec;
    private final double[] open;
    private final double[] high;
    private final double[] low;
    private final double[] close;
    private final double[] accTradePrice;
    private final double[] accTradeVolume;
    private final int size;

    private CandleSeries(
            String market,
            int unitMinutes,
            long[] candleTimeSec,
            double[] open,
            double[] high,
            double[] low,
            double[] close,
            double[] accTradePrice,
            double[] accTradeVolume,
            int size
    ) {
        this.market = market;
        this.unitMinutes = unitMinutes;
        this.candleTimeSec = candleTimeSec;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.accTradePrice = accTradePrice;
        this.accTradeVolume = accTradeVolume;
        this.size = size;
    }

    String market() {
        return market;
    }

    int unitMinutes() {
        return unitMinutes;
    }

    int size() {
        return size;
    }

    long candleTimeSec(int index) {
        return candleTimeSec[index];
    }

    /** Close-time (open time + unit) of the candle at {@code index}, in epoch seconds. */
    long closeTimeSec(int index) {
        return candleTimeSec[index] + (long) unitMinutes * 60L;
    }

    double open(int index) {
        return open[index];
    }

    double high(int index) {
        return high[index];
    }

    double low(int index) {
        return low[index];
    }

    double close(int index) {
        return close[index];
    }

    /**
     * The last {@code count} candles whose close-time is at or before {@code cursor},
     * mirroring {@code CandleProvider.getRecentCandles}. Incomplete (still-open) candles
     * relative to the simulated clock are excluded by the close-time check.
     */
    List<Candle> windowEndingAt(Instant cursor, int count) {
        long cursorSec = cursor.getEpochSecond();
        int hi = lastClosedIndex(cursorSec);
        if (hi < 0) {
            return List.of();
        }
        int from = Math.max(0, hi - count + 1);
        List<Candle> window = new ArrayList<>(hi - from + 1);
        for (int i = from; i <= hi; i++) {
            window.add(toCandle(i));
        }
        return window;
    }

    /** Index of the most recent candle whose close-time {@code <= cursorSec}, or -1. */
    int lastClosedIndex(long cursorSec) {
        // candle i is closed when candleTimeSec[i] + unit <= cursorSec
        long unitSec = (long) unitMinutes * 60L;
        int lo = 0;
        int hi = size - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (candleTimeSec[mid] + unitSec <= cursorSec) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    private Candle toCandle(int index) {
        return new Candle(
                market,
                Instant.ofEpochSecond(candleTimeSec[index]),
                BigDecimal.valueOf(open[index]),
                BigDecimal.valueOf(high[index]),
                BigDecimal.valueOf(low[index]),
                BigDecimal.valueOf(close[index]),
                BigDecimal.valueOf(accTradePrice[index]),
                BigDecimal.valueOf(accTradeVolume[index])
        );
    }

    /**
     * Streams a {@code .backtest_cache} JSON array file into a {@link CandleSeries}.
     * Uses the Jackson streaming parser so the 80 MB-per-market files never
     * materialise as a tree in memory.
     */
    static CandleSeries loadFromCache(Path cacheFile, String market, int unitMinutes) {
        long[] timeSec = new long[1 << 16];
        double[] open = new double[1 << 16];
        double[] high = new double[1 << 16];
        double[] low = new double[1 << 16];
        double[] close = new double[1 << 16];
        double[] accAmt = new double[1 << 16];
        double[] accVol = new double[1 << 16];
        int n = 0;

        try (JsonParser parser = JSON.createParser(cacheFile.toFile())) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected JSON array in " + cacheFile);
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                long t = 0;
                double o = 0;
                double h = 0;
                double l = 0;
                double c = 0;
                double amt = 0;
                double vol = 0;
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String field = parser.currentName();
                    parser.nextToken();
                    switch (field) {
                        case "candle_date_time_utc" -> t = parseUtcSeconds(parser.getText());
                        case "opening_price" -> o = parser.getDoubleValue();
                        case "high_price" -> h = parser.getDoubleValue();
                        case "low_price" -> l = parser.getDoubleValue();
                        case "trade_price" -> c = parser.getDoubleValue();
                        case "candle_acc_trade_price" -> amt = parser.getDoubleValue();
                        case "candle_acc_trade_volume" -> vol = parser.getDoubleValue();
                        default -> {
                            // skip nested structures if any
                            if (parser.currentToken().isStructStart()) {
                                parser.skipChildren();
                            }
                        }
                    }
                }
                if (n == timeSec.length) {
                    int grown = n << 1;
                    timeSec = Arrays.copyOf(timeSec, grown);
                    open = Arrays.copyOf(open, grown);
                    high = Arrays.copyOf(high, grown);
                    low = Arrays.copyOf(low, grown);
                    close = Arrays.copyOf(close, grown);
                    accAmt = Arrays.copyOf(accAmt, grown);
                    accVol = Arrays.copyOf(accVol, grown);
                }
                timeSec[n] = t;
                open[n] = o;
                high[n] = h;
                low[n] = l;
                close[n] = c;
                accAmt[n] = amt;
                accVol[n] = vol;
                n++;
            }
        }

        sortByTimeAscending(timeSec, open, high, low, close, accAmt, accVol, n);
        return new CandleSeries(market, unitMinutes, timeSec, open, high, low, close, accAmt, accVol, n);
    }

    /** Builds a series from in-memory {@link Candle}s — used by deterministic engine tests. */
    static CandleSeries ofCandles(String market, int unitMinutes, List<Candle> candles) {
        int n = candles.size();
        int cap = Math.max(1, n);
        long[] timeSec = new long[cap];
        double[] open = new double[cap];
        double[] high = new double[cap];
        double[] low = new double[cap];
        double[] close = new double[cap];
        double[] accAmt = new double[cap];
        double[] accVol = new double[cap];
        for (int i = 0; i < n; i++) {
            Candle candle = candles.get(i);
            timeSec[i] = candle.candleTime().getEpochSecond();
            open[i] = candle.openingPrice().doubleValue();
            high[i] = candle.highPrice().doubleValue();
            low[i] = candle.lowPrice().doubleValue();
            close[i] = candle.tradePrice().doubleValue();
            accAmt[i] = candle.accumulatedTradePrice().doubleValue();
            accVol[i] = candle.accumulatedTradeVolume().doubleValue();
        }
        sortByTimeAscending(timeSec, open, high, low, close, accAmt, accVol, n);
        return new CandleSeries(market, unitMinutes, timeSec, open, high, low, close, accAmt, accVol, n);
    }

    private static long parseUtcSeconds(String text) {
        return LocalDateTime.parse(text, UTC_FORMAT).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private static void sortByTimeAscending(
            long[] timeSec,
            double[] open,
            double[] high,
            double[] low,
            double[] close,
            double[] accAmt,
            double[] accVol,
            int n
    ) {
        // Upbit cache files are stored newest-first; detect and reverse rather than full sort.
        boolean ascending = true;
        for (int i = 1; i < n; i++) {
            if (timeSec[i] < timeSec[i - 1]) {
                ascending = false;
                break;
            }
        }
        if (ascending) {
            return;
        }
        boolean descending = true;
        for (int i = 1; i < n; i++) {
            if (timeSec[i] > timeSec[i - 1]) {
                descending = false;
                break;
            }
        }
        if (descending) {
            for (int i = 0, j = n - 1; i < j; i++, j--) {
                swap(timeSec, open, high, low, close, accAmt, accVol, i, j);
            }
            return;
        }
        // Mixed ordering: stable insertion-free fallback via index sort.
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        final long[] keys = timeSec.clone();
        Arrays.sort(order, (a, b) -> Long.compare(keys[a], keys[b]));
        long[] ts = timeSec.clone();
        double[] o = open.clone();
        double[] h = high.clone();
        double[] l = low.clone();
        double[] c = close.clone();
        double[] am = accAmt.clone();
        double[] vo = accVol.clone();
        for (int i = 0; i < n; i++) {
            int src = order[i];
            timeSec[i] = ts[src];
            open[i] = o[src];
            high[i] = h[src];
            low[i] = l[src];
            close[i] = c[src];
            accAmt[i] = am[src];
            accVol[i] = vo[src];
        }
    }

    private static void swap(
            long[] timeSec,
            double[] open,
            double[] high,
            double[] low,
            double[] close,
            double[] accAmt,
            double[] accVol,
            int i,
            int j
    ) {
        long t = timeSec[i];
        timeSec[i] = timeSec[j];
        timeSec[j] = t;
        swap(open, i, j);
        swap(high, i, j);
        swap(low, i, j);
        swap(close, i, j);
        swap(accAmt, i, j);
        swap(accVol, i, j);
    }

    private static void swap(double[] array, int i, int j) {
        double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
}
