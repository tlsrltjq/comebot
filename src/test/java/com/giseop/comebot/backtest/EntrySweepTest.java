package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.market.service.BtcTrendCacheService;
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
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Entry-signal experiment. The exit sweep showed no TP/SL rescues the signal, so
 * the remaining lever is the entry itself. This sweeps hypothesis-driven entry
 * filter variants (deeper pullback, stronger bounce, bigger pump, volume spike, …)
 * over the faithful replay engine with a fixed market entry + TP3/SL2 exit, and
 * reports gross (pre-cost) train/test PF.
 *
 * <p>A variant is worth pursuing only if it lifts <b>train</b> PFgross toward ≥1.15
 * without collapsing the trade count — i.e., a real, generalising signal edge, not
 * a tighter filter that just overfits a handful of trades.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.entrysweep=true --tests "*EntrySweepTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.entrysweep", matches = "true")
class EntrySweepTest {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final int TEST_WINDOW_DAYS = 60;
    private static final double TAKE_PROFIT = 3.0;
    private static final double STOP_LOSS = -2.0;

    private record Variant(String name, Consumer<CandidateScannerProperties> tune) {
    }

    private static final List<Variant> VARIANTS = List.of(
            new Variant("V0 baseline (current)", p -> { }),
            new Variant("V1 minDistFromHigh 0.5", p -> p.setMinDistanceFromHighRate(new BigDecimal("0.5"))),
            new Variant("V2 minDistFromHigh 1.0", p -> p.setMinDistanceFromHighRate(new BigDecimal("1.0"))),
            new Variant("V3 consecBullish 2", p -> p.setMinConsecutiveBullishCandles(2)),
            new Variant("V4 consecBullish 3", p -> p.setMinConsecutiveBullishCandles(3)),
            new Variant("V5 minPriceChg 0.5", p -> p.setMinPriceChangeRate(new BigDecimal("0.5"))),
            new Variant("V6 minPriceChg 1.0", p -> p.setMinPriceChangeRate(new BigDecimal("1.0"))),
            new Variant("V7 volSpike chg 30%", p -> p.setMinTradeAmountChangeRate(new BigDecimal("30"))),
            new Variant("V8 tighter ceil dist 2", p -> p.setMaxDistanceFromHighRate(new BigDecimal("2"))),
            new Variant("V9 priceRecovery 60%", p -> p.setMinPriceRecoveryRate(new BigDecimal("60"))),
            new Variant("V10 volCooldown 0.5", p -> p.setMaxVolumeCooldownRatio(new BigDecimal("0.5"))),
            new Variant("V11 combo dist0.5+consec2+vol30", p -> {
                p.setMinDistanceFromHighRate(new BigDecimal("0.5"));
                p.setMinConsecutiveBullishCandles(2);
                p.setMinTradeAmountChangeRate(new BigDecimal("30"));
            })
    );

    @Test
    void sweepEntryVariantsForGrossEdge() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        BacktestCache cache = BacktestCache.load(cacheDir);
        assumeTrue(!cache.minuteSeries().isEmpty(), "no 1m candle series in cache");
        long splitSec = cache.globalEndSec() - (long) TEST_WINDOW_DAYS * SECONDS_PER_DAY;
        BtcTrendCacheService btcTrend = new BtcTrendCacheService(cache.provider());

        System.out.println("\n==================== ENTRY-SIGNAL SWEEP (market entry, exit TP+3/SL-2) ==========");
        System.out.println("hunting: train PFgross lift toward >=1.15 without trade-count collapse");
        System.out.printf(Locale.US, "%-34s | %7s | %-13s | %-13s | %5s%n",
                "variant", "signals", "PFgross tr/te", "PFnet  tr/te", "winTe");
        System.out.println("--------------------------------------------------------------------------------------");

        double baseGross = Double.NaN;
        String bestName = null;
        double bestTrainGross = Double.NEGATIVE_INFINITY;

        for (Variant v : VARIANTS) {
            CandidateScannerProperties props = baseScannerProps();
            v.tune().accept(props);
            StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                    new StrategyProperties(), props, new StrategyMarketOverrideProperties());
            CandidateScannerService scanner = new CandidateScannerService(
                    new TradingProperties(), props, cache.provider(),
                    new VolatilityIndicatorService(), settings, null, btcTrend, null);

            BacktestEngine.Result r = run(cache, scanner, btcTrend, settings, splitSec);
            BacktestReport train = r.train();
            BacktestReport test = r.test();
            if (Double.isNaN(baseGross)) {
                baseGross = train.grossProfitFactor();
            }
            if (train.grossProfitFactor() > bestTrainGross) {
                bestTrainGross = train.grossProfitFactor();
                bestName = v.name();
            }
            System.out.printf(Locale.US, "%-34s | %7d | %6.3f/%6.3f | %6.3f/%6.3f | %4.1f%%%n",
                    v.name(), r.signals(),
                    train.grossProfitFactor(), test.grossProfitFactor(),
                    train.profitFactor(), test.profitFactor(),
                    test.winRatePct());
        }

        System.out.printf(Locale.US, "best train PFgross = %.3f (%s)   [baseline %.3f]%n",
                bestTrainGross, bestName, baseGross);
        System.out.println("==============================================================================\n");

        assertThat(bestName).isNotNull();
    }

    private BacktestEngine.Result run(
            BacktestCache cache,
            CandidateScannerService scanner,
            BtcTrendCacheService btcTrend,
            StrategyMarketSettingsService settings,
            long splitSec
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
                btcTrend, cache.provider(), settings, config).run();
    }

    private static CandidateScannerProperties baseScannerProps() {
        CandidateScannerProperties props = new CandidateScannerProperties();
        props.setCandleUnitMinutes(1);
        props.setCandleCount(20);
        props.setMinPriceChangeRate(new BigDecimal("0.15"));
        props.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        props.setMaxPriceChangeRate(new BigDecimal("10"));
        props.setMaxHighLowRangeRate(new BigDecimal("20"));
        props.setMinLatestCandleTradeAmountKrw(new BigDecimal("1000000"));
        props.setMaxDistanceFromHighRate(new BigDecimal("5"));
        props.setMinDistanceFromHighRate(BigDecimal.ZERO);
        return props;
    }
}
