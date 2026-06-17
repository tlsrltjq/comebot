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
 * Lower-timeframe maker-fill audit for the leading 15m Session Volatility candidate.
 *
 * <p>Opt-in:
 * {@code ./gradlew test -Dbacktest.sessionvolatility.submaker=true
 * --tests "*SessionVolatilityBreakoutSubCandleMakerTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.sessionvolatility.submaker", matches = "true")
class SessionVolatilityBreakoutSubCandleMakerTest {

    private static final int SIGNAL_UNIT = 15;
    private static final int[] FILL_UNITS = {5, 1};
    private static final Set<String> PEGGED_OR_STABLE = Set.of(
            "USD1USDT", "USDCUSDT", "USDEUSDT", "FDUSDUSDT", "XAUTUSDT");
    private static final Set<String> RECENT_OR_EVENT_DRIVEN = Set.of(
            "BABYUSDT", "MEGAUSDT", "NIGHTUSDT", "TRUMPUSDT");
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void sessionVolatilityBreakoutSubCandleMakerLeaderboard() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        List<CandleSeries> allSignalSeries = BacktestSeriesLoader.loadSeries(cacheDir, "BINANCE", SIGNAL_UNIT);
        assumeTrue(!allSignalSeries.isEmpty(), "no Binance 15m candle series in " + cacheDir.toAbsolutePath());
        long splitSec = BacktestSplitPolicy.splitSec(allSignalSeries.stream()
                .mapToLong(series -> series.closeTimeSec(series.size() - 1))
                .max()
                .orElseThrow());

        List<BacktestLeaderboardRow> rows = new ArrayList<>();
        for (int fillUnit : FILL_UNITS) {
            List<CandleSeries> allFillSeries = BacktestSeriesLoader.loadSeries(cacheDir, "BINANCE", fillUnit);
            assumeTrue(!allFillSeries.isEmpty(), "no Binance " + fillUnit + "m candle series");
            for (UniverseScenario universe : universeScenarios()) {
                List<CandleSeries> signalSeries = allSignalSeries.stream()
                        .filter(universe::accept)
                        .toList();
                List<CandleSeries> fillSeries = allFillSeries.stream()
                        .filter(universe::accept)
                        .toList();
                assumeTrue(!signalSeries.isEmpty(), "empty signal universe: " + universe.name());
                assumeTrue(!fillSeries.isEmpty(), "empty fill universe: " + universe.name());
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
                    BacktestEngine.Result result = new SessionVolatilityBreakoutSubCandleMakerBacktest(
                            signalSeries,
                            fillSeries,
                            config,
                            300
                    ).run();
                    rows.add(BacktestLeaderboardRow.from(
                            "session-volatility-breakout-submaker",
                            "BINANCE",
                            SIGNAL_UNIT + "m/" + fillUnit + "m",
                            BacktestSeriesLoader.marketSet(signalSeries),
                            "signal-close-limit-5m-validity",
                            params(universe, cost, fillUnit),
                            result,
                            "session=utc06-12",
                            ""));
                }
            }
        }

        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().profitFactor()).reversed()
                .thenComparing(row -> row.fillRatePct(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-submaker.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-submaker.md"), rows);

        System.out.println("\n============ SESSION VOLATILITY BREAKOUT SUB-CANDLE MAKER AUDIT ============");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.forEach(row -> System.out.println(row.toMarkdownRow()));
        System.out.println("CSV: "
                + LEADERBOARD_DIR.resolve("session-volatility-breakout-submaker.csv").toAbsolutePath());
        System.out.println("MD : "
                + LEADERBOARD_DIR.resolve("session-volatility-breakout-submaker.md").toAbsolutePath());
        System.out.println("===========================================================================\n");

        assertThat(rows).hasSize(FILL_UNITS.length * universeScenarios().size() * costScenarios().size());
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

    private static String params(UniverseScenario universe, CostScenario cost, int fillUnit) {
        return String.format(Locale.US,
                "universe=%s,cost=%s,signal=15m,fill=%dm,session=utc06-12,breakout=20,avg=60,"
                        + "minRangeRatio=2.5,minVolRatio=1.5,minCloseLocation=70.0,tp=4.0,sl=-2.0,"
                        + "maxHold=96,limitValiditySec=300,fee=%.4f/%.4f,slip=%.4f",
                universe.name(),
                cost.name(),
                fillUnit,
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
