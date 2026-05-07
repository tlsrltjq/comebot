package com.giseop.comebot.market.candle.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.market.candle.domain.Candle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BinanceCandleProviderTest {

    @Test
    void klineResponseIsMappedToCandles() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=2"))
                .andRespond(withSuccess("""
                        [
                          [
                            1714435200000,
                            "100.00",
                            "120.00",
                            "90.00",
                            "110.00",
                            "10.50",
                            1714435259999,
                            "1000000.00",
                            100,
                            "5.00",
                            "500000.00",
                            "0"
                          ],
                          [
                            1714435260000,
                            "110.00",
                            "130.00",
                            "105.00",
                            "125.00",
                            "8.20",
                            1714435319999,
                            "800000.00",
                            80,
                            "4.00",
                            "400000.00",
                            "0"
                          ]
                        ]
                        """, APPLICATION_JSON));

        List<Candle> candles = new BinanceCandleProvider(builder.build()).getRecentCandles("btcusdt", 1, 2);

        assertThat(candles).hasSize(2);
        assertThat(candles.getFirst().market()).isEqualTo("BTCUSDT");
        assertThat(candles.getFirst().openingPrice()).isEqualByComparingTo("100.00");
        assertThat(candles.getFirst().highPrice()).isEqualByComparingTo("120.00");
        assertThat(candles.getFirst().lowPrice()).isEqualByComparingTo("90.00");
        assertThat(candles.getFirst().tradePrice()).isEqualByComparingTo("110.00");
        assertThat(candles.getFirst().accumulatedTradePrice()).isEqualByComparingTo("1000000.00");
        assertThat(candles.getFirst().accumulatedTradeVolume()).isEqualByComparingTo("10.50");
        assertThat(candles.getFirst().candleTime()).isNotNull();
        server.verify();
    }

    @Test
    void apiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=5m&limit=20"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new BinanceCandleProvider(builder.build()).getRecentCandles("BTCUSDT", 5, 20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Binance candle data");
        server.verify();
    }

    @Test
    void blankMarketIsRejected() {
        BinanceCandleProvider provider = new BinanceCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles(" ", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market must not be blank");
    }

    @Test
    void unsupportedMinuteUnitIsRejected() {
        BinanceCandleProvider provider = new BinanceCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles("BTCUSDT", 10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported Binance candle minute unit: 10");
    }

    @Test
    void countOutOfRangeIsRejected() {
        BinanceCandleProvider provider = new BinanceCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles("BTCUSDT", 1, 1001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count must be between 1 and 1000");
    }
}
