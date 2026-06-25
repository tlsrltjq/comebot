package com.giseop.comebot.backtest;

import com.giseop.comebot.market.candle.domain.StockCandleImportManifest;
import com.giseop.comebot.market.candle.provider.StockCandleCsvImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class BacktestSeriesLoader {

    private BacktestSeriesLoader() {
    }

    static List<CandleSeries> loadSeries(Path cacheDir, String exchange, int unit) throws IOException {
        try (Stream<Path> files = Files.list(cacheDir)) {
            return files
                    .filter(path -> path.getFileName().toString().contains("_" + unit + "m_"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .filter(path -> matchesExchange(path.getFileName().toString(), exchange))
                    .sorted()
                    .map(path -> CandleSeries.loadFromCache(path, market(path.getFileName().toString(), unit), unit))
                    .filter(series -> series.size() > 0)
                    .toList();
        }
    }

    static CandleSeries loadStockSeries(StockCandleImportManifest manifest) throws IOException {
        return loadStockSeries(manifest, new StockCandleCsvImporter());
    }

    static CandleSeries loadStockSeries(
            StockCandleImportManifest manifest,
            StockCandleCsvImporter importer
    ) throws IOException {
        return CandleSeries.ofStockRows(
                manifest.identity().symbol(),
                manifest.interval().unitMinutes(),
                importer.load(manifest)
        );
    }

    private static boolean matchesExchange(String fileName, String exchange) {
        String market = fileName.substring(0, fileName.indexOf('_'));
        return matchesExchangeName(market, exchange);
    }

    static boolean matchesExchangeName(String market, String exchange) {
        if ("UPBIT".equals(exchange)) {
            return market.startsWith("KRW-");
        }
        return !market.startsWith("KRW-") && market.endsWith("USDT");
    }

    private static String market(String fileName, int unit) {
        return fileName.substring(0, fileName.indexOf("_" + unit + "m_"));
    }

    static String marketSet(List<CandleSeries> series) {
        return series.stream().map(CandleSeries::market).sorted().reduce((a, b) -> a + "+" + b).orElse("");
    }
}
