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
 * Robustness check for Session Volatility Breakout survivors.
 *
 * <p>Opt-in:
 * {@code ./gradlew test -Dbacktest.sessionvolatility.robustness=true
 * --tests "*SessionVolatilityBreakoutRobustnessTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.sessionvolatility.robustness", matches = "true")
class SessionVolatilityBreakoutRobustnessTest {

    private static final double MIN_CLOSE_LOCATION = 70.0;
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 96;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    private static final Set<String> PEGGED_OR_STABLE = Set.of(
            "USD1USDT", "USDCUSDT", "USDEUSDT", "FDUSDUSDT", "XAUTUSDT", "KRW-USDT");
    private static final Set<String> RECENT_OR_EVENT_DRIVEN = Set.of(
            "BABYUSDT", "MEGAUSDT", "NIGHTUSDT", "TRUMPUSDT", "KRW-MEGA", "KRW-TRUMP");

    @Test
    void sessionVolatilityBreakoutRobustnessLeaderboard() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        List<BacktestLeaderboardRow> rows = new ArrayList<>();
        for (CandidateSpec spec : candidateSpecs()) {
            List<CandleSeries> allSeries = BacktestSeriesLoader.loadSeries(cacheDir, spec.exchange(), spec.unit());
            assumeTrue(!allSeries.isEmpty(), "no candle series for " + spec.exchange() + " " + spec.unit() + "m");
            long splitSec = BacktestSplitPolicy.splitSec(allSeries.stream()
                    .mapToLong(series -> series.closeTimeSec(series.size() - 1))
                    .max()
                    .orElseThrow());
            for (UniverseScenario universe : universeScenarios()) {
                List<CandleSeries> series = allSeries.stream()
                        .filter(universe::accept)
                        .toList();
                assumeTrue(!series.isEmpty(), "empty universe: " + universe.name());
                for (CostScenario cost : costScenarios()) {
                    SessionVolatilityBreakoutConfig config = new SessionVolatilityBreakoutConfig(
                            spec.breakoutWindow(),
                            spec.averageWindow(),
                            spec.minRangeRatio(),
                            spec.minVolumeRatio(),
                            MIN_CLOSE_LOCATION,
                            spec.sessionStartHourUtc(),
                            spec.sessionEndHourUtc(),
                            TAKE_PROFIT,
                            STOP_LOSS,
                            MAX_HOLD_CANDLES,
                            1,
                            orderAmount(spec.exchange()),
                            cost.makerFeeRate(),
                            cost.takerFeeRate(),
                            cost.slippageRate(),
                            splitSec,
                            initialCapital(spec.exchange()));
                    BacktestEngine.Result result = new SessionVolatilityBreakoutBacktest(series, config).run();
                    rows.add(BacktestLeaderboardRow.from(
                            "session-volatility-breakout-robustness",
                            spec.exchange(),
                            spec.unit() + "m",
                            BacktestSeriesLoader.marketSet(series),
                            "session-breakout-next-open",
                            params(spec, universe, cost),
                            result,
                            "session=" + spec.sessionName(),
                            ""));
                }
            }
        }

        rows.sort(Comparator
                .comparing((BacktestLeaderboardRow row) -> row.test().profitFactor()).reversed()
                .thenComparing(row -> row.train().grossProfitFactor(), Comparator.reverseOrder()));
        BacktestLeaderboardWriter.writeCsv(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-robustness.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(
                LEADERBOARD_DIR.resolve("session-volatility-breakout-robustness.md"), rows);

        System.out.println("\n============= SESSION VOLATILITY BREAKOUT ROBUSTNESS =============");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: "
                + LEADERBOARD_DIR.resolve("session-volatility-breakout-robustness.csv").toAbsolutePath());
        System.out.println("MD : "
                + LEADERBOARD_DIR.resolve("session-volatility-breakout-robustness.md").toAbsolutePath());
        System.out.println("==================================================================\n");

        assertThat(rows).hasSize(candidateSpecs().size() * universeScenarios().size() * costScenarios().size());
    }

    private static List<CandidateSpec> candidateSpecs() {
        return List.of(
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 60, 60, 1.5, 3.0),
                new CandidateSpec("BINANCE", 3, "utc12-18", 12, 18, 60, 20, 1.5, 3.0),
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 20, 60, 2.5, 1.5),
                new CandidateSpec("BINANCE", 3, "utc18-24", 18, 0, 60, 60, 1.5, 1.5),
                new CandidateSpec("BINANCE", 3, "utc18-24", 18, 0, 60, 60, 1.5, 3.0),
                new CandidateSpec("BINANCE", 3, "utc12-18", 12, 18, 60, 20, 1.5, 1.5),
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 60, 20, 2.5, 3.0),
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 20, 20, 1.5, 3.0),
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 20, 60, 1.5, 3.0),
                new CandidateSpec("BINANCE", 3, "utc12-18", 12, 18, 60, 60, 1.5, 3.0),
                new CandidateSpec("BINANCE", 3, "utc18-24", 18, 0, 60, 60, 2.5, 1.5),
                new CandidateSpec("BINANCE", 15, "utc06-12", 6, 12, 60, 20, 2.5, 1.5),
                new CandidateSpec("UPBIT", 15, "utc00-06", 0, 6, 60, 60, 2.5, 3.0)
        );
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

    private static String params(CandidateSpec spec, UniverseScenario universe, CostScenario cost) {
        return String.format(Locale.US,
                "universe=%s,cost=%s,session=%s,breakout=%d,avg=%d,minRangeRatio=%.1f,"
                        + "minVolRatio=%.1f,minCloseLocation=%.1f,tp=%.1f,sl=%.1f,maxHold=%d,"
                        + "fee=%.4f/%.4f,slip=%.4f",
                universe.name(),
                cost.name(),
                spec.sessionName(),
                spec.breakoutWindow(),
                spec.averageWindow(),
                spec.minRangeRatio(),
                spec.minVolumeRatio(),
                MIN_CLOSE_LOCATION,
                TAKE_PROFIT,
                STOP_LOSS,
                MAX_HOLD_CANDLES,
                cost.makerFeeRate(),
                cost.takerFeeRate(),
                cost.slippageRate());
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }

    private record CandidateSpec(
            String exchange,
            int unit,
            String sessionName,
            int sessionStartHourUtc,
            int sessionEndHourUtc,
            int breakoutWindow,
            int averageWindow,
            double minRangeRatio,
            double minVolumeRatio
    ) {
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
