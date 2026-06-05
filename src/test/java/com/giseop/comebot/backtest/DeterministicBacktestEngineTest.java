package com.giseop.comebot.backtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Deterministic, cache-free unit tests that pin {@link BacktestEngine} mechanics on
 * hand-crafted candles: entry-signal selection, maker-limit fill vs expiry, market
 * entry at next open, TP/SL threshold exits, and gross-vs-net cost accounting.
 *
 * <p>Runs in the normal suite (no {@code .backtest_cache} needed), so the engine the
 * strategy research relies on has an assertion-level regression guard independent of
 * the slow opt-in cache replay.
 */
class DeterministicBacktestEngineTest {

    private static final ExchangeMode UPBIT = ExchangeMode.UPBIT;
    private static final String MARKET = "KRW-TEST";
    private static final long BASE_EPOCH = 1_700_000_000L;
    private static final double EPS = 1e-6;

    @Test
    void scannerSelectsOnSimpleUptrend() {
        // Precondition: the crafted uptrend is SELECTED by the real scanner, so the
        // engine tests below exercise fills/exits (not a silent no-signal path).
        List<Candle> candles = uptrend(4);
        ReplayCandleProvider provider = provider(candles);
        CandidateScannerService scanner = lenientScanner(provider);
        provider.setCursor(Instant.ofEpochSecond(candleCloseSec(candles, candles.size() - 1)));

        TradingCandidate candidate = scanner.scan(UPBIT, MARKET);

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void marketEntryFillsAtNextOpenAndTakesProfit() {
        // Rising series: select @ candle 1 → market entry at open of candle 2 (=102),
        // TP +3% threshold 105.06 first reached by candle 5 high.
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            candles.add(candle(i, 100 + i, 100 + i + 0.6, 100 + i - 0.1, 100 + i + 0.5, 1_000_000 * (1 + 0.1 * i)));
        }

        BacktestEngine.Result result = run(candles, 3.0, -2.0, true, 0, 0, 0);

        assertThat(result.closed()).hasSize(1);
        ClosedTrade trade = result.closed().get(0);
        assertThat(trade.entryPrice()).isCloseTo(102.0, within(EPS));
        assertThat(trade.exitReason()).isEqualTo("TP");
        assertThat(trade.exitPrice()).isCloseTo(102.0 * 1.03, within(102.0 * 1.03 * EPS));
        assertThat(trade.netPnl()).isPositive();
        assertThat(trade.grossPnl()).isPositive();
    }

    @Test
    void marketEntryStopsOutWhenPriceDropsBelowStopLoss() {
        // Select @ candle 1 → market entry at open of candle 2 (=102); candle 2 wicks
        // to 99.0 (< SL threshold 99.96) → stop-loss at 99.96.
        List<Candle> candles = new ArrayList<>();
        candles.add(candle(0, 100.0, 100.6, 99.9, 100.5, 1_000_000));
        candles.add(candle(1, 101.0, 101.6, 100.9, 101.5, 1_100_000));
        candles.add(candle(2, 102.0, 102.1, 99.0, 99.5, 1_200_000));
        candles.add(candle(3, 99.5, 99.8, 99.0, 99.4, 1_300_000));

        BacktestEngine.Result result = run(candles, 3.0, -2.0, true, 0, 0, 0);

        assertThat(result.closed()).hasSize(1);
        ClosedTrade trade = result.closed().get(0);
        assertThat(trade.entryPrice()).isCloseTo(102.0, within(EPS));
        assertThat(trade.exitReason()).isEqualTo("SL");
        assertThat(trade.exitPrice()).isCloseTo(102.0 * 0.98, within(102.0 * 0.98 * EPS));
        assertThat(trade.netPnl()).isNegative();
    }

    @Test
    void makerLimitFillsAtSignalCloseWhenPriceReturns() {
        // Maker mode: select @ candle 1 → limit = close(1) = 101.5; candle 2 low 101.0
        // (<= limit) fills at 101.5; candle 3 high 105 hits TP.
        List<Candle> candles = new ArrayList<>();
        candles.add(candle(0, 100.0, 100.6, 99.9, 100.5, 1_000_000));
        candles.add(candle(1, 101.0, 101.6, 100.9, 101.5, 1_100_000));
        candles.add(candle(2, 102.0, 102.2, 101.0, 102.0, 1_200_000));
        candles.add(candle(3, 102.0, 105.5, 101.9, 105.0, 1_300_000)); // fills then TP; series ends here
        // (no candle 4: ending on the TP candle avoids a second re-selected fill)

        BacktestEngine.Result result = run(candles, 3.0, -2.0, false, 0, 0, 0);

        assertThat(result.fills()).isEqualTo(1);
        assertThat(result.closed()).hasSize(1);
        ClosedTrade trade = result.closed().get(0);
        assertThat(trade.entryPrice()).isCloseTo(101.5, within(EPS)); // limit = signal close
        assertThat(trade.exitReason()).isEqualTo("TP");
    }

    @Test
    void makerLimitExpiresWhenPriceNeverReturnsToLimit() {
        // Maker mode: select @ candle 1 → limit 101.5; all later candles stay above it
        // → no fill, the pending order expires after the 5-minute validity window.
        List<Candle> candles = new ArrayList<>();
        candles.add(candle(0, 100.0, 100.6, 99.9, 100.5, 1_000_000));
        candles.add(candle(1, 101.0, 101.6, 100.9, 101.5, 1_100_000));
        for (int i = 2; i < 9; i++) {
            double base = 103 + i; // lows well above the 101.5 limit
            candles.add(candle(i, base, base + 0.6, base - 0.1, base + 0.5, 1_000_000 * (1 + 0.1 * i)));
        }

        BacktestEngine.Result result = run(candles, 3.0, -2.0, false, 0, 0, 0);

        assertThat(result.fills()).isZero();
        assertThat(result.closed()).isEmpty();
        assertThat(result.expiries()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void costsReduceNetProfitBelowGross() {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            candles.add(candle(i, 100 + i, 100 + i + 0.6, 100 + i - 0.1, 100 + i + 0.5, 1_000_000 * (1 + 0.1 * i)));
        }

        BacktestEngine.Result result = run(candles, 3.0, -2.0, true, 0.0005, 0.0005, 0.0005);

        assertThat(result.closed()).hasSize(1);
        ClosedTrade trade = result.closed().get(0);
        assertThat(trade.grossPnl()).isPositive();
        assertThat(trade.netPnl()).isLessThan(trade.grossPnl()); // fees + slippage drag
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BacktestEngine.Result run(
            List<Candle> candles,
            double takeProfit,
            double stopLoss,
            boolean marketEntry,
            double makerFee,
            double takerFee,
            double slippage
    ) {
        ReplayCandleProvider provider = provider(candles);
        CandidateScannerService scanner = lenientScanner(provider);

        PositionExitProperties exitProps = new PositionExitProperties();
        exitProps.setPositionExitEnabled(true);
        exitProps.setTakeProfitRate(BigDecimal.valueOf(takeProfit));
        exitProps.setStopLossRate(BigDecimal.valueOf(stopLoss));
        exitProps.setTrailingStopEnabled(false);
        exitProps.setAbnormalExitPriceDropRate(new BigDecimal("-50"));
        PaperPortfolioService portfolio = new PaperPortfolioService(
                new InMemoryPaperPortfolioRepository(), new PaperPortfolioProperties());
        portfolio.initialize();
        PositionExitSignalService exit = new PositionExitSignalService(exitProps, portfolio);

        CandidateScannerProperties props = lenientProps();
        StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                new StrategyProperties(), props, new StrategyMarketOverrideProperties());

        BacktestConfig config = new BacktestConfig(
                8, 8, 5, makerFee, takerFee, slippage,
                Long.MAX_VALUE, 1_000_000d, false, marketEntry);
        return new BacktestEngine(List.of(CandleSeries.ofCandles(MARKET, 1, candles)),
                scanner, exit, portfolio, null, provider, settings, config).run();
    }

    private static ReplayCandleProvider provider(List<Candle> candles) {
        ReplayCandleProvider provider = new ReplayCandleProvider();
        provider.register(CandleSeries.ofCandles(MARKET, 1, candles));
        return provider;
    }

    private static CandidateScannerService lenientScanner(ReplayCandleProvider provider) {
        CandidateScannerProperties props = lenientProps();
        StrategyMarketSettingsService settings = new StrategyMarketSettingsService(
                new StrategyProperties(), props, new StrategyMarketOverrideProperties());
        return new CandidateScannerService(
                new TradingProperties(), props, provider,
                new VolatilityIndicatorService(), settings, null, null, null);
    }

    private static CandidateScannerProperties lenientProps() {
        CandidateScannerProperties props = new CandidateScannerProperties();
        props.setCandleUnitMinutes(1);
        props.setCandleCount(3);
        props.setMinPriceChangeRate(new BigDecimal("0.01"));
        props.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        props.setMaxPriceChangeRate(new BigDecimal("1000"));
        props.setMaxHighLowRangeRate(new BigDecimal("1000"));
        props.setMinLatestCandleTradeAmountKrw(BigDecimal.ZERO);
        props.setMaxDistanceFromHighRate(new BigDecimal("1000"));
        props.setMinDistanceFromHighRate(BigDecimal.ZERO);
        return props;
    }

    private static List<Candle> uptrend(int count) {
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candles.add(candle(i, 100 + i, 100 + i + 0.6, 100 + i - 0.1, 100 + i + 0.5, 1_000_000 * (1 + 0.1 * i)));
        }
        return candles;
    }

    private static Candle candle(int minute, double open, double high, double low, double close, double amount) {
        return new Candle(
                MARKET,
                Instant.ofEpochSecond(BASE_EPOCH + (long) minute * 60L),
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                BigDecimal.valueOf(amount),
                BigDecimal.valueOf(amount / close));
    }

    private static long candleCloseSec(List<Candle> candles, int index) {
        return candles.get(index).candleTime().getEpochSecond() + 60L;
    }

    private static org.assertj.core.data.Offset<Double> within(double v) {
        return org.assertj.core.data.Offset.offset(Math.max(v, EPS));
    }
}
