package com.giseop.comebot.backtest;

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

    private static boolean matchesExchange(String fileName, String exchange) {
        String market = fileName.substring(0, fileName.indexOf('_'));
        if ("UPBIT".equals(exchange)) {
            return market.startsWith("KRW-");
        }
        return market.endsWith("USDT");
    }

    private static String market(String fileName, int unit) {
        return fileName.substring(0, fileName.indexOf("_" + unit + "m_"));
    }

    static String marketSet(List<CandleSeries> series) {
        return series.stream().map(CandleSeries::market).sorted().reduce((a, b) -> a + "+" + b).orElse("");
    }
}
