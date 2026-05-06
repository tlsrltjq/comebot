package com.giseop.comebot.mvp2.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.candle.domain.Candle;
import com.giseop.comebot.market.candle.provider.CandleProvider;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpbitExchangeMarketDataAdapterTest {

    @Test
    void adaptsTickerToExchangeTicker() {
        Instant capturedAt = Instant.parse("2026-05-06T00:00:00Z");
        UpbitExchangeMarketDataAdapter adapter = new UpbitExchangeMarketDataAdapter(
                new StubMarketPriceProvider(new MarketPrice("KRW-BTC", new BigDecimal("90000000"), capturedAt)),
                new StubCandleProvider(List.of()),
                new ExchangeSymbolNormalizer()
        );

        ExchangeTicker ticker = adapter.getTicker("krw_btc");

        assertThat(ticker.exchange()).isEqualTo(Exchange.UPBIT);
        assertThat(ticker.symbol()).isEqualTo("KRW-BTC");
        assertThat(ticker.tradePrice()).isEqualByComparingTo("90000000");
        assertThat(ticker.capturedAt()).isEqualTo(capturedAt);
    }

    @Test
    void adaptsCandlesToExchangeCandles() {
        Instant candleTime = Instant.parse("2026-05-06T00:01:00Z");
        Candle candle = new Candle(
                "KRW-ETH",
                candleTime,
                new BigDecimal("4000000"),
                new BigDecimal("4050000"),
                new BigDecimal("3990000"),
                new BigDecimal("4020000"),
                new BigDecimal("120000000"),
                new BigDecimal("30")
        );
        UpbitExchangeMarketDataAdapter adapter = new UpbitExchangeMarketDataAdapter(
                new StubMarketPriceProvider(new MarketPrice("KRW-ETH", new BigDecimal("4020000"), candleTime)),
                new StubCandleProvider(List.of(candle)),
                new ExchangeSymbolNormalizer()
        );

        List<ExchangeCandle> candles = adapter.getRecentCandles("KRW-ETH", 1, 1);

        assertThat(candles).hasSize(1);
        ExchangeCandle adapted = candles.getFirst();
        assertThat(adapted.exchange()).isEqualTo(Exchange.UPBIT);
        assertThat(adapted.symbol()).isEqualTo("KRW-ETH");
        assertThat(adapted.tradePrice()).isEqualByComparingTo("4020000");
        assertThat(adapted.accumulatedTradePrice()).isEqualByComparingTo("120000000");
    }

    @Test
    void adaptsTickerBatchWithNormalizedDistinctSymbols() {
        Instant capturedAt = Instant.parse("2026-05-06T00:00:00Z");
        StubMarketPriceProvider priceProvider = new StubMarketPriceProvider(
                new MarketPrice("KRW-BTC", new BigDecimal("90000000"), capturedAt),
                new MarketPrice("KRW-ETH", new BigDecimal("4000000"), capturedAt)
        );
        UpbitExchangeMarketDataAdapter adapter = new UpbitExchangeMarketDataAdapter(
                priceProvider,
                new StubCandleProvider(List.of()),
                new ExchangeSymbolNormalizer()
        );

        List<ExchangeTicker> tickers = adapter.getTickers(List.of("krw_btc", "KRW-BTC", "krw_eth", " "));

        assertThat(priceProvider.lastRequestedMarkets()).containsExactly("KRW-BTC", "KRW-ETH");
        assertThat(tickers)
                .extracting(ExchangeTicker::symbol)
                .containsExactly("KRW-BTC", "KRW-ETH");
    }

    private static class StubMarketPriceProvider implements MarketPriceProvider {

        private final List<MarketPrice> prices;
        private List<String> lastRequestedMarkets = List.of();

        StubMarketPriceProvider(MarketPrice... prices) {
            this.prices = List.of(prices);
        }

        @Override
        public MarketPrice getCurrentPrice(String market) {
            return prices.stream()
                    .filter(price -> price.market().equals(market))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public List<MarketPrice> getCurrentPrices(List<String> markets) {
            lastRequestedMarkets = markets;
            return markets.stream()
                    .map(this::getCurrentPrice)
                    .toList();
        }

        List<String> lastRequestedMarkets() {
            return lastRequestedMarkets;
        }
    }

    private static class StubCandleProvider implements CandleProvider {

        private final List<Candle> candles;

        StubCandleProvider(List<Candle> candles) {
            this.candles = candles;
        }

        @Override
        public List<Candle> getRecentCandles(String market, int unitMinutes, int count) {
            return candles.stream()
                    .filter(candle -> candle.market().equals(market))
                    .toList();
        }
    }
}
