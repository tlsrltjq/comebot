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
 * Coarse 15m maker-fill audit for the leading Session Volatility Breakout candidate.
 *
 * <p>Opt-in:
 * {@code ./gradlew test -Dbacktest.sessionvolatility.maker=true
 * --tests "*SessionVolatilityBreakoutMakerTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.sessionvolatility.maker", matches = "true")
class SessionVolatilityBreakoutMakerTest {

    private static final int UNIT = 15;
    private static final Set<String> PEGGED_OR_STABLE = Set.of(
            "USD1USDT", "USDCUSDT", "USDEUSDT", "FDUSDUSDT", "XAUTUSDT");
    private static final Set<String> RECENT_OR_EVENT_DRIVEN = Set.of(
            "BABYUSDT", "MEGAUSDT", "NIGHTUSDT", "TRUMPUSDT");
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void sessionVolatilityBreakoutMakerLeaderboard() throws IOException {
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
                SessionVolatilityBreakoutConfig config = new SessionVolatilityBreakoutConfig(
                        20,
                        60,
                        2.5,
                        1.5,
                        70.0,
                        6,
                        12,
                        4.0,
                        -2.0,
                        96,
                        1,
                        10d,
                        cost.makerFeeRate(),
                        cost.takerFeeRate(),
                        cost.slippageRate(),
                        splitSec,
                        1_000d);
                BacktestEngine.Result result =
                        new SessionVolatilityBreakoutMakerBacktest(series, config, 1).run();
                rows.add(BacktestLeaderboardRow.from(
                        "session-volatility-breakout-maker",
                        "BINANCE",
                        UNIT + "m",
                        BacktestSeriesLoader.marketSet(series),
                        "signal-close-limit-next-candle",
                        params(universe, cost),
                        result,
                        "session=utc06-12",
                        ""));
            }
        }

        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().profitFactor()).reversed()
                .thenComparing(row -> row.fillRatePct(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-maker.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-maker.md"), rows);

        System.out.println("\n============== SESSION VOLATILITY BREAKOUT MAKER AUDIT ==============");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.forEach(row -> System.out.println(row.toMarkdownRow()));
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("session-volatility-breakout-maker.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("session-volatility-breakout-maker.md").toAbsolutePath());
        System.out.println("======================================================================\n");

        assertThat(rows).hasSize(universeScenarios().size() * costScenarios().size());
    }

    private static List<UniverseScenario> universeScenarios() {
        return List.of(
                new UniverseScenario("all", market -> true),
                new UniverseScenario("no-pegged", market -> !PEGGED_OR_STABLE.contains(market)),
                new UniverseScenario("no-event", market -> !RECENT_OR_EVENT_DRIVEN.contains(market)),
                new UniverseScenario("no-pegged-event", market -> !PEGGED_OR_STABLE.contains(market)
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

    private static String params(UniverseScenario universe, CostScenario cost) {
        return String.format(Locale.US,
                "universe=%s,cost=%s,session=utc06-12,breakout=20,avg=60,minRangeRatio=2.5,"
                        + "minVolRatio=1.5,minCloseLocation=70.0,tp=4.0,sl=-2.0,maxHold=96,"
                        + "limitValidityCandles=1,fee=%.4f/%.4f,slip=%.4f",
                universe.name(),
                cost.name(),
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
