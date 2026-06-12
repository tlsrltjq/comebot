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
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Sweeps the exit structure (TP × SL) over the faithful replay engine and reports
 * gross (pre-cost) and net (post-cost) profit factor for train/test. The point is
 * to answer the question the parity finding raised: <b>is there any exit structure
 * where the current entry signal shows a real gross edge?</b>
 *
 * <p>Market entry is used so the signal+exit edge is isolated from maker
 * adverse-selection. A config is interesting only if it clears a gross edge large
 * enough to absorb ~0.1–0.15% round-trip costs (gross PF ≳ 1.15) AND generalises
 * (train≈test). Net PF ≥ 1.0 on the test slice would be a genuine candidate.
 *
 * <p>Opt-in: {@code ./gradlew test -Dbacktest.sweep=true --tests "*ParameterSweepTest"}.
 */
@EnabledIfSystemProperty(named = "backtest.sweep", matches = "true")
class ParameterSweepTest {

    private static final double[] TAKE_PROFITS = {2.0, 3.0, 4.0, 6.0};
    private static final double[] STOP_LOSSES = {-1.5, -2.0, -3.0};

    @Test
    void sweepExitStructureForGrossEdge() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        BacktestCache cache = BacktestCache.load(cacheDir);
        assumeTrue(!cache.minuteSeries().isEmpty(), "no 1m candle series in cache");
        long splitSec = BacktestSplitPolicy.splitSec(cache.globalEndSec());

        CandidateScannerProperties scannerProps = upbitOperatingScannerProps();
        StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                new StrategyProperties(), scannerProps, new StrategyMarketOverrideProperties());
        BtcTrendCacheService btcTrend = new BtcTrendCacheService(cache.provider());
        CandidateScannerService scanner = new CandidateScannerService(
                new TradingProperties(), scannerProps, cache.provider(),
                new VolatilityIndicatorService(), settings, null, btcTrend, null);

        System.out.println("\n==================== EXIT-STRUCTURE SWEEP (market entry, current entry filters) ====");
        System.out.println("decision policy: " + BacktestSplitPolicy.description());
        System.out.println("gross edge gate = train PFgross >= " + BacktestDecisionPolicy.MIN_GROSS_EDGE
                + " (strong >= " + BacktestDecisionPolicy.STRONG_GROSS_EDGE + ")");
        System.out.printf(Locale.US, "%4s %5s | %6s | %-13s | %-13s | %5s | %s%n",
                "TP", "SL", "trades", "PFgross tr/te", "PFnet  tr/te", "winTe", "flag");
        System.out.println("------------------------------------------------------------------------------------");

        BacktestReport bestTestGross = null;
        double bestGross = Double.NEGATIVE_INFINITY;

        for (double tp : TAKE_PROFITS) {
            for (double sl : STOP_LOSSES) {
                BacktestEngine.Result r = runConfig(cache, scanner, btcTrend, settings, splitSec, tp, sl);
                BacktestReport train = r.train();
                BacktestReport test = r.test();
                String flag = BacktestDecisionPolicy.decide(r);
                System.out.printf(Locale.US, "%+4.1f %5.1f | %6d | %6.3f/%6.3f | %6.3f/%6.3f | %4.1f%% | %s%n",
                        tp, sl, r.full().trades(),
                        train.grossProfitFactor(), test.grossProfitFactor(),
                        train.profitFactor(), test.profitFactor(),
                        test.winRatePct(), flag);
                if (test.grossProfitFactor() > bestGross) {
                    bestGross = test.grossProfitFactor();
                    bestTestGross = test;
                }
            }
        }

        System.out.printf(Locale.US, "best test PFgross = %.3f%n", bestGross);
        System.out.println("====================================================================================\n");

        assertThat(bestTestGross).isNotNull();
    }

    private BacktestEngine.Result runConfig(
            BacktestCache cache,
            CandidateScannerService scanner,
            BtcTrendCacheService btcTrend,
            StrategyMarketSettingsService settings,
            long splitSec,
            double takeProfit,
            double stopLoss
    ) {
        PositionExitProperties exitProps = new PositionExitProperties();
        exitProps.setPositionExitEnabled(true);
        exitProps.setTakeProfitRate(BigDecimal.valueOf(takeProfit));
        exitProps.setStopLossRate(BigDecimal.valueOf(stopLoss));
        exitProps.setTrailingStopEnabled(false);
        exitProps.setAbnormalExitPriceDropRate(new BigDecimal("-20"));

        PaperPortfolioService portfolio = new PaperPortfolioService(
                new InMemoryPaperPortfolioRepository(), new PaperPortfolioProperties());
        portfolio.initialize();
        PositionExitSignalService exit = new PositionExitSignalService(exitProps, portfolio);

        BacktestConfig config = new BacktestConfig(
                8, 2, 5, 0.0005, 0.0005, 0.0005, splitSec, 1_000_000d,
                false,   // intrabar tie-break irrelevant
                true);   // market entry — isolate signal+exit edge
        return new BacktestEngine(cache.minuteSeries(), scanner, exit, portfolio,
                btcTrend, cache.provider(), settings, config).run();
    }

    private static CandidateScannerProperties upbitOperatingScannerProps() {
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
