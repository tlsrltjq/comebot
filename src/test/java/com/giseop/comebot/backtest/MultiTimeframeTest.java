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
 * Multi-timeframe confirmation experiment. Unlike the BTC/market regime gates, this
 * confirms the traded coin's <i>own</i> higher-timeframe (1h) structure aligns with the
 * 1m pullback — i.e., buy the dip inside the coin's higher-TF uptrend, not a falling
 * knife. Step 1 decomposes V1 trades by coin 1h trend; step 2 adds each multi-TF
 * confirmation to V1 (and on top of the best regime gate F1) one at a time.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.mtf=true --tests "*MultiTimeframeTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.mtf", matches = "true")
class MultiTimeframeTest {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final int TEST_WINDOW_DAYS = 60;
    private static final double TAKE_PROFIT = 3.0;
    private static final double STOP_LOSS = -2.0;

    private long splitSec;

    @Test
    void multiTimeframeConfirmationOnV1() throws IOException {
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

        // ── STEP 1: does V1 loss cluster when the coin's OWN 1h trend is down? ──
        System.out.println("\n==================== V1 DECOMPOSITION BY COIN 1h TREND (multi-TF) ===============");
        System.out.printf("baseline trades=%d%n", trades.size());
        decompose("coin 1h trend", trades, t -> rc.coinHourlyTrend(t.market(), t.entryTimeSec()).name());

        // ── STEP 2: confirmations (coin HTF) alone and stacked on best regime gate F1 ──
        EntryGate f1 = (m, t) -> rc.btcTrend(t) != RegimeContext.Trend.DOWN;
        Map<String, EntryGate> experiments = new LinkedHashMap<>();
        experiments.put("baseline V1", EntryGate.ALLOW_ALL);
        experiments.put("F1 BTC!=DOWN", f1);
        experiments.put("M1 coin1h==UP", (m, t) -> rc.coinHourlyTrend(m, t) == RegimeContext.Trend.UP);
        experiments.put("M2 coin1h!=DOWN", (m, t) -> rc.coinHourlyTrend(m, t) != RegimeContext.Trend.DOWN);
        experiments.put("M3 coin 4h ret>=0", (m, t) -> !(rc.coinReturnPct(m, t, 240) < 0));
        experiments.put("F1 & M1(coin1h==UP)",
                (m, t) -> f1.allows(m, t) && rc.coinHourlyTrend(m, t) == RegimeContext.Trend.UP);
        experiments.put("F1 & M2(coin1h!=DOWN)",
                (m, t) -> f1.allows(m, t) && rc.coinHourlyTrend(m, t) != RegimeContext.Trend.DOWN);

        System.out.println("\n==================== MULTI-TF CONFIRMATION EXPERIMENTS ==========================");
        System.out.printf(Locale.US, "%-24s | %6s | %-11s | %6s | %-13s | %5s | %5s | %5s%n",
                "config", "trades", "PFgross all", "PFnet", "PFgross tr/te", "MDD", "winA", "top%");
        System.out.println("------------------------------------------------------------------------------------------");
        for (Map.Entry<String, EntryGate> e : experiments.entrySet()) {
            printRow(e.getKey(), run(cache, scanner, settings, e.getValue()));
        }
        System.out.println("==========================================================================================\n");

        assertThat(trades).isNotEmpty();
    }

    private void decompose(String dimension, List<ClosedTrade> trades, Function<ClosedTrade, String> bucketer) {
        System.out.println("-- " + dimension + " --");
        System.out.printf(Locale.US, "%-12s | %-26s | %-26s%n", "bucket", "TRAIN n/win/avgNet/PFg", "TEST n/win/avgNet/PFg");
        Map<String, List<ClosedTrade>> train = group(trades, bucketer, true);
        Map<String, List<ClosedTrade>> test = group(trades, bucketer, false);
        java.util.TreeSet<String> keys = new java.util.TreeSet<>();
        keys.addAll(train.keySet());
        keys.addAll(test.keySet());
        for (String k : keys) {
            System.out.printf(Locale.US, "%-12s | %-26s | %-26s%n", k,
                    stat(train.getOrDefault(k, List.of())), stat(test.getOrDefault(k, List.of())));
        }
    }

    private Map<String, List<ClosedTrade>> group(
            List<ClosedTrade> trades, Function<ClosedTrade, String> bucketer, boolean trainSlice) {
        Map<String, List<ClosedTrade>> map = new LinkedHashMap<>();
        for (ClosedTrade t : trades) {
            if ((t.exitTimeSec() < splitSec) != trainSlice) {
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

    private void printRow(String label, BacktestEngine.Result r) {
        BacktestReport all = r.full();
        System.out.printf(Locale.US, "%-24s | %6d | %11.3f | %6.3f | %6.3f/%6.3f | %4.1f%% | %4.1f%% | %4.1f%%%n",
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
        props.setMinDistanceFromHighRate(new BigDecimal("0.5"));
        return props;
    }
}
