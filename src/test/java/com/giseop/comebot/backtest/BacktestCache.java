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
    private final long globalEndSec;

    private BacktestCache(ReplayCandleProvider provider, List<CandleSeries> minuteSeries, long globalEndSec) {
        this.provider = provider;
        this.minuteSeries = minuteSeries;
        this.globalEndSec = globalEndSec;
    }

    ReplayCandleProvider provider() {
        return provider;
    }

    List<CandleSeries> minuteSeries() {
        return minuteSeries;
    }

    long globalEndSec() {
        return globalEndSec;
    }

    /** 1m series feed the scanner; the KRW-BTC 60m series feeds the BTC trend cache. */
    static BacktestCache load(Path cacheDir) throws IOException {
        ReplayCandleProvider provider = new ReplayCandleProvider();
        List<CandleSeries> minuteSeries = new ArrayList<>();
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
                    provider.register(CandleSeries.loadFromCache(file, market, 60));
                }
            }
        }
        return new BacktestCache(provider, minuteSeries, globalEndSec);
    }
}
