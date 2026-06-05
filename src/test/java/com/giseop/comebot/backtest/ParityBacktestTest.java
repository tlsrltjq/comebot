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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Parity backtest: replays the on-disk {@code .backtest_cache} candles through the
 * unmodified operating strategy and exit code, then prints the full / train / test
 * performance split. Because production services decide entries and exits, the PF
 * reported here is the operating engine's number — the trustworthy basis for any
 * future decision to move from PAPER to real cash.
 *
 * <p>Opt-in only (slow, ~minutes over 180 days × markets):
 * <pre>./gradlew test -Dbacktest.run=true --tests "*ParityBacktestTest"</pre>
 * It is skipped in the normal suite and auto-skips if the candle cache is absent
 * (e.g., CI), so it never blocks {@code ./gradlew test checkstyleMain}.
 */
@EnabledIfSystemProperty(named = "backtest.run", matches = "true")
class ParityBacktestTest {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final int TEST_WINDOW_DAYS = 60;

    @Test
    void replayOperatingEngineOverCachedCandles() throws IOException {
        Path cacheDir = Paths.get(System.getProperty("backtest.cacheDir", ".backtest_cache"));
        assumeTrue(Files.isDirectory(cacheDir), "candle cache not found: " + cacheDir.toAbsolutePath());

        BacktestCache cache = BacktestCache.load(cacheDir);
        ReplayCandleProvider provider = cache.provider();
        List<CandleSeries> minuteSeries = cache.minuteSeries();
        long globalEndSec = cache.globalEndSec();
        assumeTrue(!minuteSeries.isEmpty(), "no 1m candle series in cache");

        CandidateScannerProperties scannerProps = upbitOperatingScannerProps();
        StrategyProperties strategyProps = new StrategyProperties();
        StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                strategyProps, scannerProps, new StrategyMarketOverrideProperties());
        BtcTrendCacheService btcTrend = new BtcTrendCacheService(provider);

        CandidateScannerService scanner = new CandidateScannerService(
                new TradingProperties(),
                scannerProps,
                provider,
                new VolatilityIndicatorService(),
                settings,
                null,        // MarketSelectionService — unused by scan(exchange, market)
                btcTrend,
                null);       // StrategyEntryProperties — time-of-day filter disabled in ops

        long splitSec = globalEndSec - (long) TEST_WINDOW_DAYS * SECONDS_PER_DAY;

        System.out.println("\n==================== PARITY BACKTEST (operating Java engine) ====================");
        System.out.println("entry filters: current .env Upbit (1m×20, minPriceChg 0.15, dist 0-5%, minAmt 1M)");
        System.out.println("exit: TP+4.0% / SL-2.0% / trailing off (ADR-011)   costs: maker 0.05% + taker 0.05% + slip 0.05%");
        System.out.println("PFgross = pre-cost (signal edge);  PFnet = after fees+slippage (real)\n");

        // Same entry signal + exit + risk caps; only the entry FILL model differs.
        // Maker-limit is current ops (ADR-013); market-at-next-open is backtest.py's model.
        BacktestEngine.Result maker = runReplay(minuteSeries, scanner, btcTrend, provider, settings, splitSec, false);
        BacktestEngine.Result market = runReplay(minuteSeries, scanner, btcTrend, provider, settings, splitSec, true);

        System.out.println("== MAKER-LIMIT entry @signal-close, 5m valid (CURRENT OPERATIONS, ADR-013) ==");
        System.out.printf("signals=%d  fills=%d  expiries=%d  fillRate=%.1f%%%n",
                maker.signals(), maker.fills(), maker.expiries(), maker.fillRatePct());
        System.out.println(maker.full().format());
        System.out.println(maker.train().format());
        System.out.println(maker.test().format());
        System.out.println("\n== MARKET entry @next-candle-open (backtest.py model, every signal fills) ==");
        System.out.printf("signals=%d  fills=%d%n", market.signals(), market.fills());
        System.out.println(market.full().format());
        System.out.println(market.train().format());
        System.out.println(market.test().format());
        System.out.println("\n=> compare PFgross across the two entry models to isolate maker adverse-selection.");
        System.out.println("=================================================================================\n");

        assertThat(maker.fills()).isLessThanOrEqualTo(maker.signals());
        assertThat(market.fills()).isEqualTo(market.signals());
    }

    private BacktestEngine.Result runReplay(
            List<CandleSeries> series,
            CandidateScannerService scanner,
            BtcTrendCacheService btcTrend,
            ReplayCandleProvider provider,
            StrategyMarketSettingsService settings,
            long splitSec,
            boolean marketEntry
    ) {
        PaperPortfolioService portfolio = new PaperPortfolioService(
                new InMemoryPaperPortfolioRepository(), new PaperPortfolioProperties());
        portfolio.initialize();
        PositionExitSignalService exit = new PositionExitSignalService(
                upbitOperatingExitProps(), portfolio);
        BacktestConfig config = new BacktestConfig(
                8,        // maxOpenPositions (.env Upbit)
                2,        // maxBuysPerMinute (.env max-buys-per-run)
                5,        // limitValidityMinutes (ADR-013)
                0.0005,   // maker fee 0.05% (Upbit)
                0.0005,   // taker fee 0.05%
                0.0005,   // exit slippage 0.05%
                splitSec,
                1_000_000d, // UPBIT initial paper cash, for drawdown %
                false,      // intrabar tie-break irrelevant (no 1m candle spans both TP & SL)
                marketEntry);
        return new BacktestEngine(series, scanner, exit, portfolio, btcTrend, provider, settings, config).run();
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

    private static PositionExitProperties upbitOperatingExitProps() {
        PositionExitProperties props = new PositionExitProperties();
        props.setPositionExitEnabled(true);
        props.setTakeProfitRate(new BigDecimal("4.0"));
        props.setStopLossRate(new BigDecimal("-2.0"));
        props.setTrailingStopEnabled(false);
        props.setAbnormalExitPriceDropRate(new BigDecimal("-20"));
        return props;
    }
}
