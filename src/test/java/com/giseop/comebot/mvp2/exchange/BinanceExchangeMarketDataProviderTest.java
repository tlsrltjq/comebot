package com.giseop.comebot.mvp2.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BinanceExchangeMarketDataProviderTest {

    @Test
    void tickerResponseIsMappedToExchangeTicker() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withSuccess("{\"symbol\":\"BTCUSDT\",\"price\":\"64321.12\"}", APPLICATION_JSON));

        ExchangeTicker ticker = new BinanceExchangeMarketDataProvider(builder.build(), new ExchangeSymbolNormalizer())
                .getTicker("btc-usdt");

        assertThat(ticker.exchange()).isEqualTo(Exchange.BINANCE);
        assertThat(ticker.symbol()).isEqualTo("BTCUSDT");
        assertThat(ticker.tradePrice()).isEqualByComparingTo("64321.12");
        assertThat(ticker.capturedAt()).isNotNull();
        server.verify();
    }

    @Test
    void batchTickerResponseFiltersRequestedSymbols() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price"))
                .andRespond(withSuccess("""
                        [
                          {"symbol":"BTCUSDT","price":"64321.12"},
                          {"symbol":"ETHUSDT","price":"3210.45"},
                          {"symbol":"XRPUSDT","price":"0.55"}
                        ]
                        """, APPLICATION_JSON));

        List<ExchangeTicker> tickers = new BinanceExchangeMarketDataProvider(builder.build(), new ExchangeSymbolNormalizer())
                .getTickers(List.of("btc-usdt", "ETH/USDT", "btc_usdt"));

        assertThat(tickers).hasSize(2);
        assertThat(tickers)
                .extracting(ExchangeTicker::symbol)
                .containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(tickers.getFirst().tradePrice()).isEqualByComparingTo("64321.12");
        server.verify();
    }

    @Test
    void klineResponseIsMappedToExchangeCandle() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=2"))
                .andRespond(withSuccess("""
                        [
                          [
                            1714435200000,
                            "64000.00",
                            "64500.00",
                            "63900.00",
                            "64321.12",
                            "12.5",
                            1714435259999,
                            "803000.00",
                            120,
                            "6.1",
                            "392000.00",
                            "0"
                          ]
                        ]
                        """, APPLICATION_JSON));

        List<ExchangeCandle> candles = new BinanceExchangeMarketDataProvider(builder.build(), new ExchangeSymbolNormalizer())
                .getRecentCandles("BTCUSDT", 1, 2);

        assertThat(candles).hasSize(1);
        ExchangeCandle candle = candles.getFirst();
        assertThat(candle.exchange()).isEqualTo(Exchange.BINANCE);
        assertThat(candle.symbol()).isEqualTo("BTCUSDT");
        assertThat(candle.openingPrice()).isEqualByComparingTo("64000.00");
        assertThat(candle.highPrice()).isEqualByComparingTo("64500.00");
        assertThat(candle.lowPrice()).isEqualByComparingTo("63900.00");
        assertThat(candle.tradePrice()).isEqualByComparingTo("64321.12");
        assertThat(candle.accumulatedTradeVolume()).isEqualByComparingTo("12.5");
        assertThat(candle.accumulatedTradePrice()).isEqualByComparingTo("803000.00");
        server.verify();
    }

    @Test
    void tickerApiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new BinanceExchangeMarketDataProvider(builder.build(), new ExchangeSymbolNormalizer()).getTicker("BTCUSDT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Binance ticker price");
        server.verify();
    }

    @Test
    void candleApiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=5m&limit=20"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new BinanceExchangeMarketDataProvider(builder.build(), new ExchangeSymbolNormalizer())
                .getRecentCandles("BTCUSDT", 5, 20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Binance candle data");
        server.verify();
    }

    @Test
    void unsupportedMinuteUnitIsRejected() {
        BinanceExchangeMarketDataProvider provider = new BinanceExchangeMarketDataProvider(RestClient.builder().build(), new ExchangeSymbolNormalizer());

        assertThatThrownBy(() -> provider.getRecentCandles("BTCUSDT", 2, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported Binance candle minute unit: 2");
    }

    @Test
    void countOutOfRangeIsRejected() {
        BinanceExchangeMarketDataProvider provider = new BinanceExchangeMarketDataProvider(RestClient.builder().build(), new ExchangeSymbolNormalizer());

        assertThatThrownBy(() -> provider.getRecentCandles("BTCUSDT", 1, 1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count must be between 1 and 1000");
    }
}
