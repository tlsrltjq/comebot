package com.giseop.comebot.backtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads the on-disk {@code .backtest_cache} candle files once into a
 * {@link ReplayCandleProvider} + per-market 1m {@link CandleSeries}, so a single
 * load can feed many backtest runs (e.g., a parameter sweep). Pure data plumbing;
 * no strategy logic.
 */
final class BacktestCache {

    private final ReplayCandleProvider provider;
    private final List<CandleSeries> minuteSeries;
    private final CandleSeries btcHourly;
    private final long globalEndSec;

    private BacktestCache(
            ReplayCandleProvider provider,
            List<CandleSeries> minuteSeries,
            CandleSeries btcHourly,
            long globalEndSec
    ) {
        this.provider = provider;
        this.minuteSeries = minuteSeries;
        this.btcHourly = btcHourly;
        this.globalEndSec = globalEndSec;
    }

    ReplayCandleProvider provider() {
        return provider;
    }

    List<CandleSeries> minuteSeries() {
        return minuteSeries;
    }

    /** KRW-BTC 60m series, used for regime (trend/return/volatility) features. May be null. */
    CandleSeries btcHourly() {
        return btcHourly;
    }

    long globalEndSec() {
        return globalEndSec;
    }

    /** 1m series feed the scanner; the KRW-BTC 60m series feeds the BTC trend cache. */
    static BacktestCache load(Path cacheDir) throws IOException {
        ReplayCandleProvider provider = new ReplayCandleProvider();
        List<CandleSeries> minuteSeries = new ArrayList<>();
        CandleSeries btcHourly = null;
        long globalEndSec = Long.MIN_VALUE;

        try (Stream<Path> files = Files.list(cacheDir)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".json")) {
                    continue;
                }
                if (name.contains("_1m_")) {
                    String market = name.substring(0, name.indexOf("_1m_"));
                    CandleSeries s = CandleSeries.loadFromCache(file, market, 1);
                    if (s.size() == 0) {
                        continue;
                    }
                    provider.register(s);
                    minuteSeries.add(s);
                    globalEndSec = Math.max(globalEndSec, s.closeTimeSec(s.size() - 1));
                } else if (name.contains("_60m_")) {
                    String market = name.substring(0, name.indexOf("_60m_"));
                    CandleSeries hourly = CandleSeries.loadFromCache(file, market, 60);
                    provider.register(hourly);
                    if ("KRW-BTC".equals(market)) {
                        btcHourly = hourly;
                    }
                }
            }
        }
        return new BacktestCache(provider, minuteSeries, btcHourly, globalEndSec);
    }
}
