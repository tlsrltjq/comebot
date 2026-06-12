package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BacktestLeaderboardWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void rowContainsComparableMetricsAndConcentration() {
        BacktestEngine.Result result = result();

        BacktestLeaderboardRow row = BacktestLeaderboardRow.from(
                "relative-strength-momentum",
                "UPBIT",
                "1m",
                "KRW-BTC,KRW-ETH",
                "market-next-open",
                "lookback=240",
                result,
                "btc=UP",
                "candidate");

        String csv = row.toCsv();
        assertThat(BacktestLeaderboardRow.csvHeader()).contains("fullPFgross", "testPFnet", "monthlyPnl");
        assertThat(csv).contains("\"relative-strength-momentum\"");
        assertThat(csv).contains("\"KRW-BTC\"");
        assertThat(csv).contains("66.666667");
        assertThat(row.toMarkdownRow()).contains("relative-strength-momentum", "KRW-BTC 66.7%");
    }

    @Test
    void writesCsvAndMarkdownFiles() throws Exception {
        BacktestLeaderboardRow row = BacktestLeaderboardRow.from(
                "baseline",
                "BINANCE",
                "5m",
                "BTCUSDT,ETHUSDT",
                "maker-limit",
                "tp=4,sl=-2",
                result(),
                "n/a",
                "reject:PFgross<1.10");

        Path csv = tempDir.resolve("leaderboard.csv");
        Path md = tempDir.resolve("leaderboard.md");

        BacktestLeaderboardWriter.writeCsv(csv, List.of(row));
        BacktestLeaderboardWriter.writeMarkdown(md, List.of(row));

        assertThat(Files.readString(csv)).startsWith("strategy,exchange,timeframe");
        assertThat(Files.readString(csv)).contains("\"reject:PFgross<1.10\"");
        assertThat(Files.readString(md)).contains("| strategy | exchange | tf |");
        assertThat(Files.readString(md)).contains("| baseline | BINANCE | 5m |");
    }

    private static BacktestEngine.Result result() {
        List<ClosedTrade> trades = List.of(
                new ClosedTrade("KRW-BTC", 1_704_067_200L, 1_704_070_800L, 100, 104, 3000, 4000, 3, "TP"),
                new ClosedTrade("KRW-BTC", 1_706_745_600L, 1_706_749_200L, 100, 98, -2500, -2000, -2.5, "SL"),
                new ClosedTrade("KRW-ETH", 1_709_251_200L, 1_709_254_800L, 100, 106, 5000, 6000, 5, "TP")
        );
        return new BacktestEngine.Result(trades, 4, 3, 1, 1_708_000_000L, 1_000_000d);
    }
}
