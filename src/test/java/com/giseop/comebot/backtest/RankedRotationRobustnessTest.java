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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Robustness check for the weak Binance 15m ranked-rotation survivors.
 *
 * <p>Opt-in:
 * {@code ./gradlew test -Dbacktest.rankedrotation.robustness=true --tests "*RankedRotationRobustnessTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.rankedrotation.robustness", matches = "true")
class RankedRotationRobustnessTest {

    private static final int UNIT = 15;
    private static final int[] LOOKBACKS = {60, 240};
    private static final double[] MIN_RETURNS = {0.5, 1.5, 3.0};
    private static final int RANK_COUNT = 1;
    private static final int REBALANCE = 20;
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 240;
    private static final double ORDER_AMOUNT = 10d;
    private static final double INITIAL_CAPITAL = 1_000d;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    private static final Set<String> PEGGED_OR_STABLE = Set.of(
            "USD1USDT", "USDCUSDT", "USDEUSDT", "FDUSDUSDT", "XAUTUSDT");
    private static final Set<String> RECENT_OR_EVENT_DRIVEN = Set.of(
            "BABYUSDT", "MEGAUSDT", "NIGHTUSDT", "TRUMPUSDT");

    @Test
    void rankedRotationRobustnessLeaderboard() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        List<CandleSeries> allSeries = BacktestSeriesLoader.loadSeries(cacheDir, "BINANCE", UNIT);
        assumeTrue(!allSeries.isEmpty(), "no Binance 15m candle series in " + cacheDir.toAbsolutePath());
        long splitSec = BacktestSplitPolicy.splitSec(allSeries.stream()
                .mapToLong(series -> series.closeTimeSec(series.size() - 1))
                .max()
                .orElseThrow());

        List<BacktestLeaderboardRow> rows = new ArrayList<>();
        for (UniverseScenario universe : universeScenarios()) {
            List<CandleSeries> series = allSeries.stream()
                    .filter(universe::accept)
                    .toList();
            assumeTrue(!series.isEmpty(), "empty universe: " + universe.name());
            for (CostScenario cost : costScenarios()) {
                for (int lookback : LOOKBACKS) {
                    for (double minReturn : MIN_RETURNS) {
                        RankedRotationConfig config = new RankedRotationConfig(
                                lookback,
                                minReturn,
                                RANK_COUNT,
                                REBALANCE,
                                TAKE_PROFIT,
                                STOP_LOSS,
                                MAX_HOLD_CANDLES,
                                ORDER_AMOUNT,
                                cost.makerFeeRate(),
                                cost.takerFeeRate(),
                                cost.slippageRate(),
                                splitSec,
                                INITIAL_CAPITAL);
                        BacktestEngine.Result result = new RankedRotationBacktest(series, config).run();
                        rows.add(BacktestLeaderboardRow.from(
                                "ranked-rotation-robustness",
                                "BINANCE",
                                UNIT + "m",
                                BacktestSeriesLoader.marketSet(series),
                                "top1-rebalance20-next-open",
                                params(lookback, minReturn, universe, cost),
                                result,
                                "top-n-momentum",
                                ""));
                    }
                }
            }
        }

        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().profitFactor()).reversed()
                .thenComparing(row -> row.train().grossProfitFactor(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("ranked-rotation-robustness.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(LEADERBOARD_DIR.resolve("ranked-rotation-robustness.md"), rows);

        System.out.println("\n===================== RANKED ROTATION ROBUSTNESS =====================");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("ranked-rotation-robustness.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("ranked-rotation-robustness.md").toAbsolutePath());
        System.out.println("=======================================================================\n");

        assertThat(rows).hasSize(72);
    }

    private static List<UniverseScenario> universeScenarios() {
        return List.of(
                new UniverseScenario("all", market -> true),
                new UniverseScenario("no-zec", market -> !"ZECUSDT".equals(market)),
                new UniverseScenario("no-pegged", market -> !PEGGED_OR_STABLE.contains(market)),
                new UniverseScenario("no-zec-pegged-recent", market -> !"ZECUSDT".equals(market)
                        && !PEGGED_OR_STABLE.contains(market)
                        && !RECENT_OR_EVENT_DRIVEN.contains(market))
        );
    }

    private static List<CostScenario> costScenarios() {
        return List.of(
                new CostScenario("base", 0.0005, 0.0005, 0.0005),
                new CostScenario("conservative", 0.0010, 0.0010, 0.0010),
                new CostScenario("stress", 0.0010, 0.0010, 0.0020)
        );
    }

    private static String params(
            int lookback,
            double minReturn,
            UniverseScenario universe,
            CostScenario cost
    ) {
        return String.format(Locale.US,
                "universe=%s,cost=%s,lookback=%d,rankCount=%d,rebalance=%d,minReturn=%.1f,"
                        + "tp=%.1f,sl=%.1f,maxHold=%d,fee=%.4f/%.4f,slip=%.4f",
                universe.name(),
                cost.name(),
                lookback,
                RANK_COUNT,
                REBALANCE,
                minReturn,
                TAKE_PROFIT,
                STOP_LOSS,
                MAX_HOLD_CANDLES,
                cost.makerFeeRate(),
                cost.takerFeeRate(),
                cost.slippageRate());
    }

    private record UniverseScenario(String name, MarketFilter filter) {
        boolean accept(CandleSeries series) {
            return filter.accept(series.market());
        }
    }

    private record CostScenario(String name, double makerFeeRate, double takerFeeRate, double slippageRate) {
    }

    @FunctionalInterface
    private interface MarketFilter {
        boolean accept(String market);
    }
}
