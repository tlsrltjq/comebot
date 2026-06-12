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
 * Third non-pullback strategy candidate: trade a close breakout after a low-range
 * contraction window.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.vcbreakout=true --tests "*VolatilityContractionBreakoutTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.vcbreakout", matches = "true")
class VolatilityContractionBreakoutTest {

    private static final int[] UNITS = {1, 3, 5, 15};
    private static final int[] CONTRACTION_WINDOWS = {10, 20, 60};
    private static final double[] MAX_AVG_RANGES = {0.4, 0.8, 1.2};
    private static final double[] MIN_BREAKOUTS = {0.2, 0.5, 1.0};
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 96;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void volatilityContractionBreakoutLeaderboard() throws IOException {
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
                for (int window : CONTRACTION_WINDOWS) {
                    for (double maxRange : MAX_AVG_RANGES) {
                        for (double breakout : MIN_BREAKOUTS) {
                            VolatilityContractionBreakoutConfig config = new VolatilityContractionBreakoutConfig(
                                    window,
                                    maxRange,
                                    window,
                                    breakout,
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
                            BacktestEngine.Result result = new VolatilityContractionBreakoutBacktest(series, config).run();
                            rows.add(BacktestLeaderboardRow.from(
                                    "volatility-contraction-breakout",
                                    exchange,
                                    unit + "m",
                                    BacktestSeriesLoader.marketSet(series),
                                    "market-next-open",
                                    params(window, maxRange, breakout),
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
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("volatility-contraction-breakout.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(LEADERBOARD_DIR.resolve("volatility-contraction-breakout.md"), rows);

        System.out.println("\n==================== VOLATILITY-CONTRACTION BREAKOUT LEADERBOARD ===============");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("volatility-contraction-breakout.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("volatility-contraction-breakout.md").toAbsolutePath());
        System.out.println("================================================================================\n");

        assertThat(rows).isNotEmpty();
    }

    private static String params(int window, double maxRange, double breakout) {
        return String.format(Locale.US,
                "window=%d,maxAvgRange=%.1f,minBreakout=%.1f,tp=%.1f,sl=%.1f,maxHold=%d",
                window, maxRange, breakout, TAKE_PROFIT, STOP_LOSS, MAX_HOLD_CANDLES);
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }
}
