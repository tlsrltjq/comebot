package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import com.giseop.comebot.strategy.service.StrategyMarketOverrideProperties;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import java.math.BigDecimal;
import java.time.Instant;
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
        assertThat(candidate.reason()).isEqualTo("Volatility long candidate selected");
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
                )
        );

        TradingCandidate candidate = exchangeAwareService.scan(ExchangeMode.BINANCE, "BTCUSDT");

        assertThat(candidate.market()).isEqualTo("BTCUSDT");
        assertThat(upbitProvider.requestedMarkets).isEmpty();
        assertThat(binanceProvider.requestedMarkets).containsExactly("BTCUSDT");
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
        assertThat(candidate.reason()).isEqualTo("Trend is not UP");
    }

    @Test
    void lowPriceChangeRateIsSkipped() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("5"));
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "102", "99", "101", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "101", "103", "100", "102", "1200")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Price change rate is below threshold");
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
    void providerFailureIsReturnedAsSkippedCandidate() {
        candleProvider.failure = true;

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Candidate scan failed: IllegalStateException - failed");
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
        return new Candle(
                market,
                Instant.parse(time),
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

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            requestedMarkets.add(market);
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
