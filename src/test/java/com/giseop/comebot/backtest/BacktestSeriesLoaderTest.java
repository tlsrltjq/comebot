package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.CandleInterval;
import com.giseop.comebot.market.candle.domain.StockCandleImportManifest;
import com.giseop.comebot.market.domain.MarketIdentity;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BacktestSeriesLoaderTest {

    @Test
    void excludesUpbitKrwUsdtMarketFromBinanceUniverse() {
        assertThat(BacktestSeriesLoader.matchesExchangeName("KRW-USDT", "UPBIT")).isTrue();
        assertThat(BacktestSeriesLoader.matchesExchangeName("KRW-USDT", "BINANCE")).isFalse();
        assertThat(BacktestSeriesLoader.matchesExchangeName("BTCUSDT", "BINANCE")).isTrue();
        assertThat(BacktestSeriesLoader.matchesExchangeName("BTCUSDT", "UPBIT")).isFalse();
    }

    @Test
    void loadsStockCsvManifestIntoBacktestSeries(@TempDir Path tempDir) throws IOException {
        Path dataFile = tempDir.resolve("AAPL.csv");
        Files.writeString(dataFile, """
                timestamp,open,high,low,close,volume
                2026-01-02T14:30:00Z,100,102,99,101,1000
                2026-01-02T14:45:00Z,101,103,100,102,1100
                """);
        StockCandleImportManifest manifest = new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("aapl"),
                CandleInterval.FIFTEEN_MINUTES,
                MarketIdentity.usStock("aapl").venue().timezone(),
                true,
                false,
                Instant.parse("2026-01-02T14:30:00Z"),
                Instant.parse("2026-01-02T15:00:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                dataFile
        );

        CandleSeries series = BacktestSeriesLoader.loadStockSeries(manifest);

        assertThat(series.market()).isEqualTo("AAPL");
        assertThat(series.unitMinutes()).isEqualTo(15);
        assertThat(series.size()).isEqualTo(2);
        assertThat(series.close(1)).isEqualTo(102.0);
        assertThat(series.accTradePrice(1)).isEqualTo(112_200.0);
    }

    @Test
    void loadsBundledSampleStockCsv() throws IOException, URISyntaxException {
        URL resource = Objects.requireNonNull(
                getClass().getResource("/stock/us/sample/15m/AAPL.csv")
        );
        StockCandleImportManifest manifest = new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.FIFTEEN_MINUTES,
                MarketIdentity.usStock("AAPL").venue().timezone(),
                true,
                false,
                Instant.parse("2026-01-02T14:30:00Z"),
                Instant.parse("2026-01-02T15:15:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                Path.of(resource.toURI())
        );

        CandleSeries series = BacktestSeriesLoader.loadStockSeries(manifest);

        assertThat(series.market()).isEqualTo("AAPL");
        assertThat(series.unitMinutes()).isEqualTo(15);
        assertThat(series.size()).isEqualTo(3);
        assertThat(series.windowEndingAt(Instant.parse("2026-01-02T15:15:00Z"), 2))
                .extracting(candle -> candle.tradePrice().intValue())
                .containsExactly(102, 103);
    }
}
