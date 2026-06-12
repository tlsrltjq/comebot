package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Fourth non-pullback strategy candidate: buy a sharp oversold move and exit on
 * short mean reversion.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.oversoldmr=true --tests "*OversoldMeanReversionTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.oversoldmr", matches = "true")
class OversoldMeanReversionTest {

    private static final int[] UNITS = {1, 3, 5, 15};
    private static final int[] LOOKBACKS = {10, 20, 60};
    private static final double[] MIN_DROPS = {2.0, 4.0, 7.0};
    private static final double[] MIN_DEVIATIONS = {1.0, 2.0, 4.0};
    private static final double TAKE_PROFIT = 2.0;
    private static final double STOP_LOSS = -2.5;
    private static final double MAX_CURRENT_RANGE = 5.0;
    private static final int MAX_HOLD_CANDLES = 48;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void oversoldMeanReversionLeaderboard() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        List<BacktestLeaderboardRow> rows = new ArrayList<>();
        for (String exchange : List.of("UPBIT", "BINANCE")) {
            for (int unit : UNITS) {
                List<CandleSeries> series = BacktestSeriesLoader.loadSeries(cacheDir, exchange, unit);
                if (series.isEmpty()) {
                    continue;
                }
                long globalEndSec = series.stream()
                        .mapToLong(s -> s.closeTimeSec(s.size() - 1))
                        .max()
                        .orElseThrow();
                long splitSec = BacktestSplitPolicy.splitSec(globalEndSec);
                for (int lookback : LOOKBACKS) {
                    for (double drop : MIN_DROPS) {
                        for (double deviation : MIN_DEVIATIONS) {
                            OversoldMeanReversionConfig config = new OversoldMeanReversionConfig(
                                    lookback,
                                    drop,
                                    deviation,
                                    MAX_CURRENT_RANGE,
                                    TAKE_PROFIT,
                                    STOP_LOSS,
                                    MAX_HOLD_CANDLES,
                                    1,
                                    orderAmount(exchange),
                                    0.0005,
                                    0.0005,
                                    0.0005,
                                    splitSec,
                                    initialCapital(exchange));
                            BacktestEngine.Result result = new OversoldMeanReversionBacktest(series, config).run();
                            rows.add(BacktestLeaderboardRow.from(
                                    "oversold-mean-reversion",
                                    exchange,
                                    unit + "m",
                                    BacktestSeriesLoader.marketSet(series),
                                    "market-next-open",
                                    params(lookback, drop, deviation),
                                    result,
                                    "n/a",
                                    ""));
                        }
                    }
                }
            }
        }

        assumeTrue(!rows.isEmpty(), "no matching candle series in " + cacheDir.toAbsolutePath());
        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().grossProfitFactor()).reversed()
                .thenComparing(row -> row.full().trades(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("oversold-mean-reversion.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(LEADERBOARD_DIR.resolve("oversold-mean-reversion.md"), rows);

        System.out.println("\n====================== OVERSOLD MEAN REVERSION LEADERBOARD =====================");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("oversold-mean-reversion.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("oversold-mean-reversion.md").toAbsolutePath());
        System.out.println("================================================================================\n");

        assertThat(rows).isNotEmpty();
    }

    private static String params(int lookback, double drop, double deviation) {
        return String.format(Locale.US,
                "lookback=%d,minDrop=%.1f,minDeviation=%.1f,maxRange=%.1f,tp=%.1f,sl=%.1f,maxHold=%d",
                lookback, drop, deviation, MAX_CURRENT_RANGE, TAKE_PROFIT, STOP_LOSS, MAX_HOLD_CANDLES);
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }
}
