package com.giseop.comebot.strategy.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import com.giseop.comebot.strategy.indicator.VolatilityIndicatorService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandidateScannerServiceTest {

    private final TradingProperties tradingProperties = new TradingProperties();
    private final CandidateScannerProperties scannerProperties = new CandidateScannerProperties();
    private final StubCandleProvider candleProvider = new StubCandleProvider();
    private final CandidateScannerService service = new CandidateScannerService(
            tradingProperties,
            scannerProperties,
            candleProvider,
            new VolatilityIndicatorService()
    );

    @Test
    void upwardVolatilityMarketIsSelected() {
        scannerProperties.setMinPriceChangeRate(new BigDecimal("1.5"));
        scannerProperties.setMinTradeAmountChangeRate(new BigDecimal("10"));
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
        candleProvider.candles = List.of(
                candle("KRW-BTC", "2026-04-30T00:00:00Z", "100", "110", "95", "105", "1000"),
                candle("KRW-BTC", "2026-04-30T00:01:00Z", "105", "125", "104", "120", "1100")
        );

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Trade amount change rate is below threshold");
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
        assertThat(candleProvider.requestedMarkets).containsExactly("KRW-BTC", "KRW-ETH");
    }

    @Test
    void providerFailureIsReturnedAsSkippedCandidate() {
        candleProvider.failure = true;

        TradingCandidate candidate = service.scan("KRW-BTC");

        assertThat(candidate.decision()).isEqualTo(CandidateDecision.SKIPPED);
        assertThat(candidate.reason()).isEqualTo("Candidate scan failed: IllegalStateException");
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
        private final java.util.ArrayList<String> requestedMarkets = new java.util.ArrayList<>();

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
