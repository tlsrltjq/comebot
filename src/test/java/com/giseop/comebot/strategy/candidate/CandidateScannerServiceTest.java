package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.service.StrategyEntryProperties;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import com.giseop.comebot.strategy.service.StrategyType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateScannerServiceTest {

    private final TradingProperties tradingProperties = new TradingProperties();
    private final StrategyProperties strategyProperties = new StrategyProperties();
    private final CandidateScannerProperties scannerProperties = new CandidateScannerProperties();
    private final StrategyMarketOverrideProperties overrideProperties = new StrategyMarketOverrideProperties();
    private final StubCandleProvider candleProvider = new StubCandleProvider();
    private final CandidateScannerService service = new CandidateScannerService(
            tradingProperties,
            scannerProperties,
            candleProvider,
            new VolatilityIndicatorService(),
            new StrategyMarketSettingsService(strategyProperties, scannerProperties, overrideProperties)
    );

    @Test
    void upwardVolatilityMarketIsSelected() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1.5"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(candidate.trend()).isEqualTo(MarketTrend.UP);
        assertThat(candidate.priceChangeRate()).isEqualByComparingTo("20.0000");
        assertThat(candidate.reason()).isEqualTo("Pullback bounce candidate selected");
    }

    @Test
    void binanceScanUsesBinanceCandleProvider() {
        StubCandleProvider upbitProvider = new StubCandleProvider();
        StubCandleProvider binanceProvider = new StubCandleProvider();
        binanceProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        CandidateScannerService exchangeAwareService = new CandidateScannerService(
                tradingProperties,
                scannerProperties,
                upbitProvider,
                binanceProvider,
                new VolatilityIndicatorService(),
                new StrategyMarketSettingsService(strategyProperties, scannerProperties, overrideProperties),
                new com.giseop.comebot.market.service.MarketSelectionService(
                        new com.giseop.comebot.market.service.UpbitKrwTickerStore(),
                        new com.giseop.comebot.market.service.BinanceUsdtTickerStore()
                ),
                null
        );

        TradingCandidate candidate = exchangeAwareService.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.market()).isEqualTo("BTCUSDT");
        assertThat(upbitProvider.requestedMarkets).isEmpty();
        assertThat(binanceProvider.requestedMarkets).containsExactly("BTCUSDT");
    }

    @Test
    void sessionVolatilityBreakoutSelectionDelegatesToSessionScanner() {
        StrategySelectionProperties selectionProperties = new StrategySelectionProperties();
        selectionProperties.setSelected(StrategyType.SESSION_VOLATILITY_BREAKOUT);
        SessionVolatilityBreakoutScannerService sessionScanner = mock(SessionVolatilityBreakoutScannerService.class);
        TradingCandidate selected = new TradingCandidate(
                "BTCUSDT",
                CandidateDecision.SELECTED,
                "Session volatility breakout selected: Binance 15m UTC06-12 close-limit",
                new BigDecimal("109"),
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                MarketTrend.UP,
                true,
                Instant.parse("2026-06-15T06:15:00Z")
        );
        when(sessionScanner.scan(ExchangeMode.BINANCE, "BTCUSDT")).thenReturn(selected);
        CandidateScannerService selectedStrategyService = new CandidateScannerService(
                tradingProperties,
                scannerProperties,
                candleProvider,
                candleProvider,
                new VolatilityIndicatorService(),
                new StrategyMarketSettingsService(strategyProperties, scannerProperties, overrideProperties),
                new com.giseop.comebot.market.service.MarketSelectionService(
                        new com.giseop.comebot.market.service.UpbitKrwTickerStore(),
                        new com.giseop.comebot.market.service.BinanceUsdtTickerStore()
                ),
                null,
                null,
                selectionProperties,
                sessionScanner,
                CLOCK_AT_KST_14
        );

        TradingCandidate candidate = selectedStrategyService.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate).isEqualTo(selected);
        verify(sessionScanner).scan(ExchangeMode.BINANCE, "BTCUSDT");
        assertThat(candleProvider.requestedMarkets).isEmpty();
    }

    @Test
    void exchangeSpecificCandleCountsAreUsed() {
        StubCandleProvider upbitProvider = new StubCandleProvider();
        StubCandleProvider binanceProvider = new StubCandleProvider();
        upbitProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        binanceProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        CandidateScannerProperties properties = new CandidateScannerProperties();
        CandidateScannerProperties.ExchangeSettings upbit = new CandidateScannerProperties.ExchangeSettings();
        upbit.setCandleCount(20);
        properties.setUpbit(upbit);
        CandidateScannerProperties.ExchangeSettings binance = new CandidateScannerProperties.ExchangeSettings();
        binance.setCandleCount(10);
        properties.setBinance(binance);
        CandidateScannerService exchangeAwareService = new CandidateScannerService(
                tradingProperties,
                properties,
                upbitProvider,
                binanceProvider,
                new VolatilityIndicatorService(),
                new StrategyMarketSettingsService(strategyProperties, properties, overrideProperties),
                new com.giseop.comebot.market.service.MarketSelectionService(
                        new com.giseop.comebot.market.service.UpbitKrwTickerStore(),
                        new com.giseop.comebot.market.service.BinanceUsdtTickerStore()
                ),
                null
        );

        exchangeAwareService.scan(ExchangeMode.UPBIT, "KRW-BTC");
        exchangeAwareService.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(upbitProvider.requestedCounts).containsExactly(20);
        assertThat(binanceProvider.requestedCounts).containsExactly(10);
    }

    @Test
    void exchangeSpecificLatestTradeAmountThresholdsAreUsed() {
        StubCandleProvider upbitProvider = new StubCandleProvider();
        StubCandleProvider binanceProvider = new StubCandleProvider();
        upbitProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        binanceProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        CandidateScannerProperties properties = new CandidateScannerProperties();
        properties.setMinPriceChangeRate(new BigDecimal("1"));
        properties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        properties.setMaxPriceChangeRate(new BigDecimal("30"));
        properties.setMaxHighLowRangeRate(new BigDecimal("40"));
        CandidateScannerProperties.ExchangeSettings upbit = new CandidateScannerProperties.ExchangeSettings();
        upbit.setMinLatestCandleTradeAmountKrw(new BigDecimal("1000"));
        properties.setUpbit(upbit);
        CandidateScannerProperties.ExchangeSettings binance = new CandidateScannerProperties.ExchangeSettings();
        binance.setMinLatestCandleTradeAmountUsdt(new BigDecimal("2000"));
        properties.setBinance(binance);
        CandidateScannerService exchangeAwareService = new CandidateScannerService(
                tradingProperties,
                properties,
                upbitProvider,
                binanceProvider,
                new VolatilityIndicatorService(),
                new StrategyMarketSettingsService(strategyProperties, properties, overrideProperties),
                new com.giseop.comebot.market.service.MarketSelectionService(
                        new com.giseop.comebot.market.service.UpbitKrwTickerStore(),
                        new com.giseop.comebot.market.service.BinanceUsdtTickerStore()
                ),
                null
        );

        TradingCandidate upbitCandidate = exchangeAwareService.scan(ExchangeMode.UPBIT, "KRW-BTC");
        TradingCandidate binanceCandidate = exchangeAwareService.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(upbitCandidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(binanceCandidate.reason()).isEqualTo("Latest candle trade amount is below minimum threshold");
    }


    @Test
    void zeroTradeAmountCandlesAreSkippedBeforeIndicatorCalculation() {
        scannerProperties.setMinPriceChangeRate(BigDecimal.ZERO);
        scannerProperties.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "0"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "120", "100", "115", "1000"),
                candle("BTCUSDT", "2026-04-30T00:02:00Z", "115", "130", "110", "125", "1500")
        );

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(candidate.market()).isEqualTo("BTCUSDT");
    }

    @Test
    void returnsHoldWhenNotEnoughPositiveTradeAmountCandlesRemain() {
        candleProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "0"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "120", "100", "115", "0")
        );

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Not enough valid trade amount candles");
    }

    @Test
    void bearishLastCandleIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1.5"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "120", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:04:00Z", "118", "125", "104", "110", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Last candle is not bullish");
    }

    @Test
    void downTrendIsSkipped() {
        // Both candles are declining — net priceChangeRate is negative
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "90", "95", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "95", "100", "80", "90", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Trend is not UP");
    }

    @Test
    void sidewaysMarketIsSkipped() {
        // Price returns to start — last candle close equals open → not bullish
        scannerProperties.setMinPriceChangeRate(BigDecimal.ZERO);
        scannerProperties.setMinTradeAmountChangeRate(BigDecimal.ZERO);
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "100", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "100", "110", "95", "100", "1000")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.trend()).isEqualTo(MarketTrend.SIDEWAYS);
        assertThat(candidate.reason()).isEqualTo("Last candle is not bullish");
    }

    @Test
    void lowPriceChangeRateIsSkipped() {
        // windowHighChangeRate (3%) is below minPriceChangeRate (5%)
        scannerProperties.setMinPriceChangeRate(new BigDecimal("5"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "102", "99", "101", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "101", "103", "100", "102", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("No significant pump detected in window");
    }

    @Test
    void lowTradeAmountChangeRateIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1100")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Trade amount change rate is below threshold");
    }

    @Test
    void overheatedPriceChangeRateIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("10"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Price change rate is overheated");
    }

    @Test
    void overheatedHighLowRangeRateIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("10"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("High low range rate is overheated");
    }

    @Test
    void marketOverrideThresholdsAreApplied() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("30"));
        StrategyMarketOverrideProperties.MarketOverride override = new StrategyMarketOverrideProperties.MarketOverride();
        override.setMinPriceChangeRate(new BigDecimal("1"));
        override.setMaxPriceChangeRate(new BigDecimal("30"));
        override.setMaxHighLowRangeRate(new BigDecimal("40"));
        overrideProperties.setMarkets(java.util.Map.of("KRW-BTC", override));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void scansAllowedMarketsOnly() {
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", " ", "KRW-ETH"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        List<TradingCandidate> candidates = service.scanAllowedMarkets();

        assertThat(candidates).hasSize(2);
        assertThat(candleProvider.requestedMarkets).containsExactlyInAnyOrder("KRW-BTC", "KRW-ETH");
    }

    @Test
    void scansAllowedMarketsWithLimit() {
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        List<TradingCandidate> candidates = service.scanAllowedMarkets(ExchangeMode.UPBIT, 1);

        assertThat(candidates).hasSize(1);
        assertThat(candleProvider.requestedMarkets).containsExactly("KRW-BTC");
    }

    @Test
    void latestCandleTradeAmountBelowKrwThresholdIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinLatestCandleTradeAmountKrw(new BigDecimal("2000"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Latest candle trade amount is below minimum threshold");
    }

    @Test
    void latestCandleTradeAmountBelowUsdtThresholdIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinLatestCandleTradeAmountUsdt(new BigDecimal("2000"));
        candleProvider.candles = List.of(
                candle("BTCUSDT", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("BTCUSDT", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Latest candle trade amount is below minimum threshold");
    }

    @Test
    void priceTooFarBelowHighIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMaxDistanceFromHighRate(new BigDecimal("2"));
        // latest close (120) is 4% below the high (125) → should be rejected
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Price is too far below the recent high");
    }

    @Test
    void priceCloseToHighPassesFilter() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMaxDistanceFromHighRate(new BigDecimal("2"));
        // latest close (124) is 0.8% below the high (125) → should be selected
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "124", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void pullbackBounceIsSelectedWhenPriceIsInPullbackZone() {
        // Pump to 125, then pullback to 122 (2.4% from high) → within [0.5%, 3%] zone + bullish last candle
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinDistanceFromHighRate(new BigDecimal("0.5"));
        scannerProperties.setMaxDistanceFromHighRate(new BigDecimal("3"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "99", "102", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "102", "125", "101", "121", "1500"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "121", "123", "120", "122", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void pullbackAtPeakIsRejectedWhenMinDistanceNotMet() {
        // Price is still at the peak — no pullback yet (distanceFromHigh = 0% < minDistance 0.5%)
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinDistanceFromHighRate(new BigDecimal("0.5"));
        scannerProperties.setMaxDistanceFromHighRate(new BigDecimal("3"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "99", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "125", "1500")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Price has not pulled back sufficiently from high");
    }

    @Test
    void latestCandleTradeAmountAboveThresholdIsNotBlocked() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinLatestCandleTradeAmountKrw(new BigDecimal("1000"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void incompleteLatestCandleIsIgnoredForEntryDecision() {
        Instant currentMinute = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinLatestCandleTradeAmountKrw(new BigDecimal("1000"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", currentMinute.minusSeconds(120), "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", currentMinute.minusSeconds(60), "105", "125", "104", "120", "1200"),
                candle("KRW-BTC", currentMinute, "120", "121", "119", "119", "1")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
        assertThat(candidate.currentPrice()).isEqualByComparingTo("120");
    }

    @Test
    void volumeNotCooledDownIsSkippedWhenFilterEnabled() {
        // peak trade amount = 2000 (second candle), latest = 1800 → ratio 0.9 > threshold 0.5
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMaxVolumeCooldownRatio(new BigDecimal("0.5"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "120", "122", "119", "121", "1800")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Volume has not cooled down after peak");
    }

    @Test
    void cooledVolumeIsSelectedWhenFilterEnabled() {
        // peak trade amount = 2000 (second candle), latest = 500 → ratio 0.25 ≤ threshold 0.5
        // last candle: open=119 close=121 (bullish)
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMaxVolumeCooldownRatio(new BigDecimal("0.5"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "122", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "119", "123", "119", "121", "500")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void insufficientConsecutiveBullishCandlesIsSkipped() {
        // pump(bullish) → pullback(bearish) → bounce start(bullish): only 1 consecutive, filter requires 2
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinConsecutiveBullishCandles(2);
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95",  "105", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "114", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "114", "116", "108", "109", "1200"),
                candle("KRW-BTC", "2026-04-30T00:03:00Z", "109", "113", "108", "112", "600")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Not enough consecutive bullish candles");
    }

    @Test
    void sufficientConsecutiveBullishCandlesIsSelected() {
        // 2 consecutive bullish at end, filter requires 2
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinConsecutiveBullishCandles(2);
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "119", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "119", "122", "118", "121", "600")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void lowPriceRecoveryRateIsSkipped() {
        // window low=99, high=128, latest close=102 → recovery=(102-99)/(128-99)*100 ≈ 10.3% < threshold 50%
        // overall trend positive: oldest.open=100, latest.close=102 → +2%
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinPriceRecoveryRate(new BigDecimal("50"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "125", "100", "102", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "125", "128", "100", "102", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "100", "105", "99",  "102", "600")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Price recovery rate is below threshold");
    }

    @Test
    void sufficientPriceRecoveryRateIsSelected() {
        // low=90, high=125, latest close=115 → recovery=(115-90)/(125-90)*100 ≈ 71.4% > threshold 50%
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMinPriceRecoveryRate(new BigDecimal("50"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "125", "90", "112", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "112", "118", "111", "115", "2000")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void volumeCooldownFilterIsSkippedWhenSetToZero() {
        // ratio > 0.5 but filter is disabled (maxVolumeCooldownRatio = 0)
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        scannerProperties.setMaxVolumeCooldownRatio(BigDecimal.ZERO);
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "500"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "2000"),
                candle("KRW-BTC", "2026-04-30T00:02:00Z", "120", "122", "119", "121", "1800")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void providerFailureIsReturnedAsSkippedCandidate() {
        candleProvider.failure = true;

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Candidate scan failed: IllegalStateException - failed");
    }

    // 2026-04-30T05:00:00Z == 14:00 KST
    private static final Clock CLOCK_AT_KST_14 =
            Clock.fixed(Instant.parse("2026-04-30T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    private CandidateScannerService timeFilteredService(StrategyEntryProperties entryProps, Clock clock) {
        return new CandidateScannerService(
                tradingProperties,
                scannerProperties,
                candleProvider,
                candleProvider,
                new VolatilityIndicatorService(),
                new StrategyMarketSettingsService(strategyProperties, scannerProperties, overrideProperties),
                new com.giseop.comebot.market.service.MarketSelectionService(
                        new com.giseop.comebot.market.service.UpbitKrwTickerStore()),
                null,
                entryProps,
                clock
        );
    }

    @Test
    void entryIsSelectedWhenWithinAllowedTradingHour() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1.5"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        StrategyEntryProperties entryProps = new StrategyEntryProperties();
        entryProps.setAllowedHoursKst(List.of(14));

        TradingCandidate candidate = timeFilteredService(entryProps, CLOCK_AT_KST_14).scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SELECTED);
    }

    @Test
    void entryIsSkippedWhenOutsideAllowedTradingHour() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1.5"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
        scannerProperties.setMaxPriceChangeRate(new BigDecimal("30"));
        scannerProperties.setMaxHighLowRangeRate(new BigDecimal("40"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1200")
        );
        StrategyEntryProperties entryProps = new StrategyEntryProperties();
        entryProps.setAllowedHoursKst(List.of(3)); // 14 KST is not allowed

        TradingCandidate candidate = timeFilteredService(entryProps, CLOCK_AT_KST_14).scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Outside allowed trading hours (KST)");
    }

    private Candle candle(
            String market,
            String time,
            String open,
            String high,
            String low,
            String trade,
            String tradeAmount
    ) {
        return candle(market, Instant.parse(time), open, high, low, trade, tradeAmount);
    }

    private Candle candle(
            String market,
            Instant time,
            String open,
            String high,
            String low,
            String trade,
            String tradeAmount
    ) {
        return new Candle(
                market,
                time,
                new BigDecimal(open),
                new BigDecimal(high),
                new BigDecimal(low),
                new BigDecimal(trade),
                new BigDecimal(tradeAmount),
                BigDecimal.ONE
        );
    }

    private static class StubCandleProvider implements CandleProvider {

        private List<Candle> candles = List.of();
        private boolean failure = false;
        private final List<String> requestedMarkets = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        private final List<Integer> requestedCounts = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            requestedMarkets.add(market);
            requestedCounts.add(count);
            if (failure) {
                throw new IllegalStateException("failed");
            }
            return candles.stream()
                    .map(candle -> new Candle(
                            market,
                            candle.candleTime(),
                            candle.openingPrice(),
                            candle.highPrice(),
                            candle.lowPrice(),
                            candle.tradePrice(),
                            candle.accumulatedTradePrice(),
                            candle.accumulatedTradeVolume()
                    ))
                    .toList();
        }
    }
}
