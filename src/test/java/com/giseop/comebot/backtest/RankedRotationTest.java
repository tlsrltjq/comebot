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
 * Ranked rotation candidate: rebalance into the top N markets by lookback return
 * and rotate out markets that fall out of the target set.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.rankedrotation=true --tests "*RankedRotationTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.rankedrotation", matches = "true")
class RankedRotationTest {

    private static final int[] UNITS = {1, 3, 5, 15};
    private static final int[] LOOKBACKS = {20, 60, 240};
    private static final int[] RANK_COUNTS = {1, 3};
    private static final int[] REBALANCE_INTERVALS = {20, 60};
    private static final double[] MIN_RETURNS = {0.5, 1.5, 3.0};
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 240;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void rankedRotationLeaderboard() throws IOException {
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
                    for (int rankCount : RANK_COUNTS) {
                        for (int rebalance : REBALANCE_INTERVALS) {
                            for (double minReturn : MIN_RETURNS) {
                                RankedRotationConfig config = new RankedRotationConfig(
                                        lookback,
                                        minReturn,
                                        rankCount,
                                        rebalance,
                                        TAKE_PROFIT,
                                        STOP_LOSS,
                                        MAX_HOLD_CANDLES,
                                        orderAmount(exchange),
                                        0.0005,
                                        0.0005,
                                        0.0005,
                                        splitSec,
                                        initialCapital(exchange));
                                BacktestEngine.Result result = new RankedRotationBacktest(series, config).run();
                                rows.add(BacktestLeaderboardRow.from(
                                        "ranked-rotation",
                                        exchange,
                                        unit + "m",
                                        BacktestSeriesLoader.marketSet(series),
                                        "top-n-rebalance-next-open",
                                        params(lookback, rankCount, rebalance, minReturn),
                                        result,
                                        "top-n-momentum",
                                        ""));
                            }
                        }
                    }
                }
            }
        }

        assumeTrue(!rows.isEmpty(), "no matching candle series in " + cacheDir.toAbsolutePath());
        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().grossProfitFactor()).reversed()
                .thenComparing(row -> row.full().trades(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("ranked-rotation.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(LEADERBOARD_DIR.resolve("ranked-rotation.md"), rows);

        System.out.println("\n=========================== RANKED ROTATION LEADERBOARD ========================");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("ranked-rotation.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("ranked-rotation.md").toAbsolutePath());
        System.out.println("================================================================================\n");

        assertThat(rows).isNotEmpty();
    }

    private static String params(int lookback, int rankCount, int rebalance, double minReturn) {
        return String.format(Locale.US,
                "lookback=%d,rankCount=%d,rebalance=%d,minReturn=%.1f,tp=%.1f,sl=%.1f,maxHold=%d",
                lookback, rankCount, rebalance, minReturn, TAKE_PROFIT, STOP_LOSS, MAX_HOLD_CANDLES);
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }
}
