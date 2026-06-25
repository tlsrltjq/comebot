package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.CandleInterval;
import com.giseop.comebot.market.candle.domain.StockCandleImportManifest;
import com.giseop.comebot.market.domain.MarketIdentity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StockOpeningRangeBreakoutBacktestTest {

    @Test
    void producesOfflineStockBreakoutTradeFromImportedCsv(@TempDir Path tempDir) throws IOException {
        CandleSeries series = stockSeries(tempDir, """
                timestamp,open,high,low,close,volume
                2026-01-02T14:30:00Z,100,101,99,100,1000
                2026-01-02T14:45:00Z,100,102,100,101,1200
                2026-01-02T15:00:00Z,101,104,101,103,1400
                2026-01-02T15:15:00Z,103,106,102,105,1500
                """);

        List<ClosedTrade> trades = new StockOpeningRangeBreakoutBacktest(series, defaultConfig()).run();

        assertThat(trades).hasSize(1);
        ClosedTrade trade = trades.getFirst();
        assertThat(trade.market()).isEqualTo("AAPL");
        assertThat(trade.entryPrice()).isEqualTo(103.0);
        assertThat(trade.exitReason()).isEqualTo("TP");
        assertThat(trade.grossPnl()).isPositive();
        assertThat(trade.netPnl()).isPositive();
        assertThat(BacktestReport.of("stock", trades, 10_000).profitFactor()).isInfinite();
    }

    @Test
    void appliesRoundTripCostToWeakBreakout(@TempDir Path tempDir) throws IOException {
        CandleSeries series = stockSeries(tempDir, """
                timestamp,open,high,low,close,volume
                2026-01-02T14:30:00Z,100,101,99,100,1000
                2026-01-02T14:45:00Z,100,102,100,101,1200
                2026-01-02T15:00:00Z,101,103,101,102.5,1400
                2026-01-02T15:15:00Z,102.5,103,102,102.6,1500
                """);
        StockOpeningRangeBreakoutBacktest.Config config =
                new StockOpeningRangeBreakoutBacktest.Config(2, 1, 4.0, 4.0, 10.0, 1_000);

        List<ClosedTrade> trades = new StockOpeningRangeBreakoutBacktest(series, config).run();

        assertThat(trades).hasSize(1);
        assertThat(trades.getFirst().exitReason()).isEqualTo("TIME");
        assertThat(trades.getFirst().grossPnl()).isPositive();
        assertThat(trades.getFirst().netPnl()).isNegative();
    }

    private static StockOpeningRangeBreakoutBacktest.Config defaultConfig() {
        return new StockOpeningRangeBreakoutBacktest.Config(2, 2, 1.5, 1.0, 5.0, 1_000);
    }

    private static CandleSeries stockSeries(Path tempDir, String csv) throws IOException {
        Path dataFile = tempDir.resolve("AAPL.csv");
        Files.writeString(dataFile, csv);
        StockCandleImportManifest manifest = new StockCandleImportManifest(
                "sample",
                MarketIdentity.usStock("AAPL"),
                CandleInterval.FIFTEEN_MINUTES,
                MarketIdentity.usStock("AAPL").venue().timezone(),
                true,
                false,
                Instant.parse("2026-01-02T14:30:00Z"),
                Instant.parse("2026-01-02T15:30:00Z"),
                Instant.parse("2026-01-03T00:00:00Z"),
                dataFile
        );
        return BacktestSeriesLoader.loadStockSeries(manifest);
    }
}
