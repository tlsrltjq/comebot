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
 * Session-specific volatility expansion candidate.
 *
 * <p>Opt-in:
 * {@code ./gradlew test -Dbacktest.sessionvolatility=true --tests "*SessionVolatilityBreakoutTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.sessionvolatility", matches = "true")
class SessionVolatilityBreakoutTest {

    private static final int[] UNITS = {1, 3, 5, 15};
    private static final int[] BREAKOUT_WINDOWS = {20, 60};
    private static final int[] AVERAGE_WINDOWS = {20, 60};
    private static final double[] MIN_RANGE_RATIOS = {1.5, 2.5};
    private static final double[] MIN_VOLUME_RATIOS = {1.5, 3.0};
    private static final double MIN_CLOSE_LOCATION = 70.0;
    private static final Session[] SESSIONS = {
        new Session("all", 0, 0),
        new Session("utc00-06", 0, 6),
        new Session("utc06-12", 6, 12),
        new Session("utc12-18", 12, 18),
        new Session("utc18-24", 18, 0)
    };
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 96;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void sessionVolatilityBreakoutLeaderboard() throws IOException {
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
                for (Session session : SESSIONS) {
                    for (int breakoutWindow : BREAKOUT_WINDOWS) {
                        for (int averageWindow : AVERAGE_WINDOWS) {
                            for (double rangeRatio : MIN_RANGE_RATIOS) {
                                for (double volumeRatio : MIN_VOLUME_RATIOS) {
                                    SessionVolatilityBreakoutConfig config = new SessionVolatilityBreakoutConfig(
                                            breakoutWindow,
                                            averageWindow,
                                            rangeRatio,
                                            volumeRatio,
                                            MIN_CLOSE_LOCATION,
                                            session.startHourUtc(),
                                            session.endHourUtc(),
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
                                    BacktestEngine.Result result =
                                            new SessionVolatilityBreakoutBacktest(series, config).run();
                                    rows.add(BacktestLeaderboardRow.from(
                                            "session-volatility-breakout",
                                            exchange,
                                            unit + "m",
                                            BacktestSeriesLoader.marketSet(series),
                                            "session-breakout-next-open",
                                            params(session, breakoutWindow, averageWindow, rangeRatio, volumeRatio),
                                            result,
                                            "session=" + session.name(),
                                            ""));
                                }
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
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("session-volatility-breakout.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(
                LEADERBOARD_DIR.resolve("session-volatility-breakout.md"), rows);

        System.out.println("\n================== SESSION VOLATILITY BREAKOUT LEADERBOARD ==================");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("session-volatility-breakout.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("session-volatility-breakout.md").toAbsolutePath());
        System.out.println("=============================================================================\n");

        assertThat(rows).isNotEmpty();
    }

    private static String params(
            Session session,
            int breakoutWindow,
            int averageWindow,
            double rangeRatio,
            double volumeRatio
    ) {
        return String.format(Locale.US,
                "session=%s,breakout=%d,avg=%d,minRangeRatio=%.1f,minVolRatio=%.1f,"
                        + "minCloseLocation=%.1f,tp=%.1f,sl=%.1f,maxHold=%d",
                session.name(),
                breakoutWindow,
                averageWindow,
                rangeRatio,
                volumeRatio,
                MIN_CLOSE_LOCATION,
                TAKE_PROFIT,
                STOP_LOSS,
                MAX_HOLD_CANDLES);
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }

    private record Session(String name, int startHourUtc, int endHourUtc) {
    }
}
