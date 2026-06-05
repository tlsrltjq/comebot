package com.giseop.comebot.backtest;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.UpbitCandleProvider;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link UpbitCandleProvider} that serves historical candles up to a moving
 * simulation cursor instead of hitting the Upbit REST API.
 *
 * <p>Because the operating {@code CandidateScannerService} and
 * {@code BtcTrendCacheService} only ever obtain candles through
 * {@code getRecentCandles}, swapping this provider in lets the unmodified
 * production strategy/exit code run against the on-disk backtest cache — the
 * whole point of a parity backtest.
 *
 * <p>The same instance answers both the scanner's 1-minute requests and the BTC
 * trend cache's 60-minute requests; it keys series by {@code market + "|" + unit}.
 */
final class ReplayCandleProvider extends UpbitCandleProvider {

    private final Map<String, CandleSeries> seriesByKey = new HashMap<>();
    private final AtomicReference<Instant> cursor = new AtomicReference<>(Instant.EPOCH);

    void register(CandleSeries series) {
        seriesByKey.put(key(series.market(), series.unitMinutes()), series);
    }

    void setCursor(Instant instant) {
        cursor.set(instant);
    }

    @Override
    public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
        CandleSeries series = seriesByKey.get(key(market, unitMinutes));
        if (series == null) {
            return List.of();
        }
        return series.windowEndingAt(cursor.get(), count);
    }

    private static String key(String market, int unitMinutes) {
        return market + "|" + unitMinutes;
    }
}
