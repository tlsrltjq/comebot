package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Regime experiment on the V1 pullback entry (minDistFromHigh ≥ 0.5%). Step 1
 * decomposes V1 trades by BTC trend / recent return / volatility / market breadth
 * (train/test split) to test whether losses cluster in down/risk-off regimes.
 * Step 3 adds each minimal regime {@link EntryGate} to V1 one at a time and compares
 * gross/net + train/test PF, trade count, MDD, avg P&L, win-rate, and coin concentration.
 *
 * <p>Market entry + TP+3/SL−2 fixed (isolate the signal+regime effect). Opt-in:
 * {@code ./gradlew test -Dbacktest.regime=true --tests "*RegimeAnalysisTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.regime", matches = "true")
class RegimeAnalysisTest {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final int TEST_WINDOW_DAYS = 60;
    private static final double TAKE_PROFIT = 3.0;
    private static final double STOP_LOSS = -2.0;
    private static final int RETURN_HOURS = 6;
    private static final int VOL_HOURS = 24;

    private long splitSec;

    @Test
    void decomposeAndSweepRegimeFilters() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());
        BacktestCache cache = BacktestCache.load(cacheDir);
        assumeTrue(!cache.minuteSeries().isEmpty(), "no 1m candle series in cache");
        assumeTrue(cache.btcHourly() != null, "no KRW-BTC 60m series in cache");
        splitSec = cache.globalEndSec() - (long) TEST_WINDOW_DAYS * SECONDS_PER_DAY;

        RegimeContext rc = new RegimeContext(cache.btcHourly(), cache.minuteSeries());
        CandidateScannerProperties props = v1ScannerProps();
        StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                new StrategyProperties(), props, new StrategyMarketOverrideProperties());
        CandidateScannerService scanner = new CandidateScannerService(
                new TradingProperties(), props, cache.provider(),
                new VolatilityIndicatorService(), settings, null, null, null);

        BacktestEngine.Result base = run(cache, scanner, settings, EntryGate.ALLOW_ALL);
        List<ClosedTrade> trades = base.closed();
        double medianVol = median(trades.stream()
                .mapToDouble(t -> rc.btcVolatilityPct(t.entryTimeSec(), VOL_HOURS))
                .filter(v -> !Double.isNaN(v)).toArray());

        // ============================== STEP 1: DECOMPOSITION ==============================
        System.out.println("\n==================== V1 TRADE DECOMPOSITION BY REGIME (market entry, TP3/SL2) ====");
        System.out.printf("baseline trades=%d  (train/test split = last %dd)  medianBTCvol(%dh)=%.3f%%%n",
                trades.size(), TEST_WINDOW_DAYS, VOL_HOURS, medianVol);

        decompose("BTC 1h trend", trades, t -> rc.btcTrend(t.entryTimeSec()).name());
        decompose("BTC " + RETURN_HOURS + "h return", trades, t -> {
            double r = rc.btcReturnPct(t.entryTimeSec(), RETURN_HOURS);
            return Double.isNaN(r) ? "n/a" : r >= 0 ? "up(>=0)" : "down(<0)";
        });
        decompose("BTC " + VOL_HOURS + "h volatility", trades, t -> {
            double v = rc.btcVolatilityPct(t.entryTimeSec(), VOL_HOURS);
            return Double.isNaN(v) ? "n/a" : v >= medianVol ? "high" : "low";
        });
        decompose("market breadth(1h up)", trades, t -> {
            double b = rc.breadthUp(t.entryTimeSec());
            return Double.isNaN(b) ? "n/a" : b >= 0.5 ? "broad(>=50%)" : "narrow(<50%)";
        });

        // ============================== STEP 2: FILTER CANDIDATES ==========================
        Map<String, EntryGate> filters = new LinkedHashMap<>();
        filters.put("F1 BTC trend != DOWN", (m, t) -> rc.btcTrend(t) != RegimeContext.Trend.DOWN);
        filters.put("F2 BTC trend == UP only", (m, t) -> rc.btcTrend(t) == RegimeContext.Trend.UP);
        filters.put("F3 BTC " + RETURN_HOURS + "h return >= 0",
                (m, t) -> !(rc.btcReturnPct(t, RETURN_HOURS) < 0));
        filters.put("F4 not(highVol & BTC ret<0)", (m, t) -> {
            double v = rc.btcVolatilityPct(t, VOL_HOURS);
            double r = rc.btcReturnPct(t, RETURN_HOURS);
            return !(v >= medianVol && r < 0);
        });
        // Two independently-stable signals (BTC direction + cross-sectional breadth).
        filters.put("F5 breadth >= 50%", (m, t) -> !(rc.breadthUp(t) < 0.5));
        filters.put("F6 BTC!=DOWN & breadth>=50%",
                (m, t) -> rc.btcTrend(t) != RegimeContext.Trend.DOWN && !(rc.breadthUp(t) < 0.5));

        // ============================== STEP 3: SINGLE-FILTER EXPERIMENTS =================
        System.out.println("\n==================== SINGLE REGIME-FILTER EXPERIMENTS (one at a time) ============");
        System.out.printf(Locale.US, "%-28s | %6s | %-13s | %6s | %-13s | %5s | %5s | %5s%n",
                "config", "trades", "PFgross all", "PFnet", "PFgross tr/te", "MDD", "winA", "top%");
        System.out.println("----------------------------------------------------------------------------------------------");
        printRow("baseline V1", base);
        for (Map.Entry<String, EntryGate> f : filters.entrySet()) {
            printRow(f.getKey(), run(cache, scanner, settings, f.getValue()));
        }
        System.out.println("==============================================================================================\n");

        assertThat(trades).isNotEmpty();
    }

    // ── decomposition ────────────────────────────────────────────────────────

    private void decompose(String dimension, List<ClosedTrade> trades, Function<ClosedTrade, String> bucketer) {
        System.out.println("\n-- " + dimension + " --");
        System.out.printf(Locale.US, "%-16s | %-28s | %-28s%n", "bucket", "TRAIN n/win/avgNet/PFg", "TEST n/win/avgNet/PFg");
        Map<String, List<ClosedTrade>> train = group(trades, bucketer, true);
        Map<String, List<ClosedTrade>> test = group(trades, bucketer, false);
        java.util.TreeSet<String> keys = new java.util.TreeSet<>();
        keys.addAll(train.keySet());
        keys.addAll(test.keySet());
        for (String k : keys) {
            System.out.printf(Locale.US, "%-16s | %-28s | %-28s%n", k,
                    stat(train.getOrDefault(k, List.of())), stat(test.getOrDefault(k, List.of())));
        }
    }

    private Map<String, List<ClosedTrade>> group(
            List<ClosedTrade> trades, Function<ClosedTrade, String> bucketer, boolean trainSlice) {
        Map<String, List<ClosedTrade>> map = new LinkedHashMap<>();
        for (ClosedTrade t : trades) {
            boolean isTrain = t.exitTimeSec() < splitSec;
            if (isTrain != trainSlice) {
                continue;
            }
            map.computeIfAbsent(bucketer.apply(t), key -> new ArrayList<>()).add(t);
        }
        return map;
    }

    private static String stat(List<ClosedTrade> ts) {
        if (ts.isEmpty()) {
            return String.format(Locale.US, "%4d  -      -        -", 0);
        }
        int n = ts.size();
        long wins = ts.stream().filter(ClosedTrade::isWin).count();
        double avgNet = ts.stream().mapToDouble(ClosedTrade::netPnl).average().orElse(0);
        double gp = ts.stream().filter(ClosedTrade::isGrossWin).mapToDouble(ClosedTrade::grossPnl).sum();
        double gl = ts.stream().filter(t -> !t.isGrossWin()).mapToDouble(t -> Math.abs(t.grossPnl())).sum();
        double pf = gl == 0 ? Double.POSITIVE_INFINITY : gp / gl;
        return String.format(Locale.US, "%4d  %4.1f%%  %+8.0f  %5.3f", n, wins * 100.0 / n, avgNet, pf);
    }

    // ── experiment table ─────────────────────────────────────────────────────

    private void printRow(String label, BacktestEngine.Result r) {
        BacktestReport all = r.full();
        System.out.printf(Locale.US, "%-28s | %6d | %11.3f | %6.3f | %6.3f/%6.3f | %4.1f%% | %4.1f%% | %4.1f%%%n",
                label, all.trades(), all.grossProfitFactor(), all.profitFactor(),
                r.train().grossProfitFactor(), r.test().grossProfitFactor(),
                all.maxDrawdownPct(), all.winRatePct(), topCoinShare(r.closed()) * 100.0);
    }

    private static double topCoinShare(List<ClosedTrade> trades) {
        if (trades.isEmpty()) {
            return 0;
        }
        Map<String, Integer> byMarket = new LinkedHashMap<>();
        for (ClosedTrade t : trades) {
            byMarket.merge(t.market(), 1, Integer::sum);
        }
        return byMarket.values().stream().mapToInt(Integer::intValue).max().orElse(0) / (double) trades.size();
    }

    private static double median(double[] values) {
        if (values.length == 0) {
            return Double.NaN;
        }
        double[] copy = values.clone();
        java.util.Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    // ── engine wiring ────────────────────────────────────────────────────────

    private BacktestEngine.Result run(
            BacktestCache cache,
            CandidateScannerService scanner,
            StrategyMarketSettingsService settings,
            EntryGate gate
    ) {
        PositionExitProperties exitProps = new PositionExitProperties();
        exitProps.setPositionExitEnabled(true);
        exitProps.setTakeProfitRate(BigDecimal.valueOf(TAKE_PROFIT));
        exitProps.setStopLossRate(BigDecimal.valueOf(STOP_LOSS));
        exitProps.setTrailingStopEnabled(false);
        exitProps.setAbnormalExitPriceDropRate(new BigDecimal("-20"));
        PaperPortfolioService portfolio = new PaperPortfolioService(
                new InMemoryPaperPortfolioRepository(), new PaperPortfolioProperties());
        portfolio.initialize();
        PositionExitSignalService exit = new PositionExitSignalService(exitProps, portfolio);

        BacktestConfig config = new BacktestConfig(
                8, 2, 5, 0.0005, 0.0005, 0.0005, splitSec, 1_000_000d, false, true);
        return new BacktestEngine(cache.minuteSeries(), scanner, exit, portfolio,
                null, cache.provider(), settings, config, gate).run();
    }

    private static CandidateScannerProperties v1ScannerProps() {
        CandidateScannerProperties props = new CandidateScannerProperties();
        props.setCandleUnitMinutes(1);
        props.setCandleCount(20);
        props.setMinPriceChangeRate(new BigDecimal("0.15"));
        props.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        props.setMaxPriceChangeRate(new BigDecimal("10"));
        props.setMaxHighLowRangeRate(new BigDecimal("20"));
        props.setMinLatestCandleTradeAmountKrw(new BigDecimal("1000000"));
        props.setMaxDistanceFromHighRate(new BigDecimal("5"));
        props.setMinDistanceFromHighRate(new BigDecimal("0.5")); // V1: pullback-depth floor
        return props;
    }
}
