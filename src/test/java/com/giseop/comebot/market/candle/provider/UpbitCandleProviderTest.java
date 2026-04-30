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

class UpbitCandleProviderTest {

    @Test
    void minuteCandleResponseIsMappedToDomain() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/candles/minutes/1?market=KRW-BTC&count=2"))
                .andRespond(withSuccess("""
                        [
                          {
                            "market": "KRW-BTC",
                            "timestamp": 1714435200000,
                            "opening_price": 100,
                            "high_price": 120,
                            "low_price": 90,
                            "trade_price": 110,
                            "candle_acc_trade_price": 1000000,
                            "candle_acc_trade_volume": 10.5
                          },
                          {
                            "market": "KRW-BTC",
                            "timestamp": 1714435140000,
                            "opening_price": 95,
                            "high_price": 105,
                            "low_price": 91,
                            "trade_price": 100,
                            "candle_acc_trade_price": 800000,
                            "candle_acc_trade_volume": 8.2
                          }
                        ]
                        """, APPLICATION_JSON));

        List<Candle> candles = new UpbitCandleProvider(builder.build()).getRecentCandles("KRW-BTC", 1, 2);

        assertThat(candles).hasSize(2);
        assertThat(candles.getFirst().market()).isEqualTo("KRW-BTC");
        assertThat(candles.getFirst().openingPrice()).isEqualByComparingTo("100");
        assertThat(candles.getFirst().highPrice()).isEqualByComparingTo("120");
        assertThat(candles.getFirst().lowPrice()).isEqualByComparingTo("90");
        assertThat(candles.getFirst().tradePrice()).isEqualByComparingTo("110");
        assertThat(candles.getFirst().accumulatedTradePrice()).isEqualByComparingTo("1000000");
        assertThat(candles.getFirst().accumulatedTradeVolume()).isEqualByComparingTo("10.5");
        assertThat(candles.getFirst().candleTime()).isNotNull();
        server.verify();
    }

    @Test
    void apiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/candles/minutes/5?market=KRW-BTC&count=20"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new UpbitCandleProvider(builder.build()).getRecentCandles("KRW-BTC", 5, 20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Upbit candle data");
        server.verify();
    }

    @Test
    void blankMarketIsRejected() {
        UpbitCandleProvider provider = new UpbitCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles(" ", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("market must not be blank");
    }

    @Test
    void unsupportedMinuteUnitIsRejected() {
        UpbitCandleProvider provider = new UpbitCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles("KRW-BTC", 2, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported candle minute unit: 2");
    }

    @Test
    void countOutOfRangeIsRejected() {
        UpbitCandleProvider provider = new UpbitCandleProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getRecentCandles("KRW-BTC", 1, 201))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("count must be between 1 and 200");
    }
}
