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
 * Fifth non-pullback strategy candidate: only take market momentum when BTC is in
 * a matching positive regime.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.btcmarketregime=true --tests "*BtcMarketRegimeMomentumTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.btcmarketregime", matches = "true")
class BtcMarketRegimeMomentumTest {

    private static final int[] UNITS = {1, 3, 5, 15};
    private static final int[] BTC_LOOKBACKS = {20, 60, 240};
    private static final int[] MARKET_LOOKBACKS = {20, 60};
    private static final double[] MIN_BTC_RETURNS = {0.5, 1.5, 3.0};
    private static final double[] MIN_MARKET_RETURNS = {1.0, 2.0, 4.0};
    private static final double TAKE_PROFIT = 4.0;
    private static final double STOP_LOSS = -2.0;
    private static final int MAX_HOLD_CANDLES = 96;
    private static final Path LEADERBOARD_DIR = Paths.get("build/backtest-leaderboard");

    @Test
    void btcMarketRegimeMomentumLeaderboard() throws IOException {
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
                for (int btcLookback : BTC_LOOKBACKS) {
                    for (int marketLookback : MARKET_LOOKBACKS) {
                        for (double btcReturn : MIN_BTC_RETURNS) {
                            for (double marketReturn : MIN_MARKET_RETURNS) {
                                BtcMarketRegimeMomentumConfig config = new BtcMarketRegimeMomentumConfig(
                                        btcLookback,
                                        btcReturn,
                                        marketLookback,
                                        marketReturn,
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
                                BacktestEngine.Result result = new BtcMarketRegimeMomentumBacktest(series, config).run();
                                rows.add(BacktestLeaderboardRow.from(
                                        "btc-market-regime-momentum",
                                        exchange,
                                        unit + "m",
                                        BacktestSeriesLoader.marketSet(series),
                                        "market-next-open",
                                        params(btcLookback, marketLookback, btcReturn, marketReturn),
                                        result,
                                        "btc-up+market-up",
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
        BacktestLeaderboardWriter.writeCsv(LEADERBOARD_DIR.resolve("btc-market-regime-momentum.csv"), rows);
        BacktestLeaderboardWriter.writeMarkdown(LEADERBOARD_DIR.resolve("btc-market-regime-momentum.md"), rows);

        System.out.println("\n===================== BTC-MARKET REGIME MOMENTUM LEADERBOARD ==================");
        System.out.println(BacktestLeaderboardRow.markdownHeader());
        rows.stream().limit(12).map(BacktestLeaderboardRow::toMarkdownRow).forEach(System.out::println);
        System.out.println("CSV: " + LEADERBOARD_DIR.resolve("btc-market-regime-momentum.csv").toAbsolutePath());
        System.out.println("MD : " + LEADERBOARD_DIR.resolve("btc-market-regime-momentum.md").toAbsolutePath());
        System.out.println("================================================================================\n");

        assertThat(rows).isNotEmpty();
    }

    private static String params(int btcLookback, int marketLookback, double btcReturn, double marketReturn) {
        return String.format(Locale.US,
                "btcLookback=%d,marketLookback=%d,minBtcReturn=%.1f,minMarketReturn=%.1f,tp=%.1f,sl=%.1f,maxHold=%d",
                btcLookback, marketLookback, btcReturn, marketReturn, TAKE_PROFIT, STOP_LOSS, MAX_HOLD_CANDLES);
    }

    private static double orderAmount(String exchange) {
        return "BINANCE".equals(exchange) ? 10d : 10_000d;
    }

    private static double initialCapital(String exchange) {
        return "BINANCE".equals(exchange) ? 1_000d : 1_000_000d;
    }
}
