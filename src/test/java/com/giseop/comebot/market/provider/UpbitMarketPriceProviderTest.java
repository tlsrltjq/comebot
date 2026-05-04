package com.giseop.comebot.market.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.market.domain.MarketPrice;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UpbitMarketPriceProviderTest {

    @Test
    void tradePriceIsMappedToCurrentPrice() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker?markets=KRW-BTC"))
                .andRespond(withSuccess("[{\"market\":\"KRW-BTC\",\"trade_price\":123456789.12,\"acc_trade_price_24h\":1}]", APPLICATION_JSON));

        MarketPrice price = new UpbitMarketPriceProvider(builder.build()).getCurrentPrice("KRW-BTC");

        assertThat(price.market()).isEqualTo("KRW-BTC");
        assertThat(price.currentPrice()).isEqualByComparingTo("123456789.12");
        assertThat(price.capturedAt()).isNotNull();
        server.verify();
    }

    @Test
    void batchTickerRequestMapsMultipleMarkets() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker?markets=KRW-BTC,KRW-ETH"))
                .andRespond(withSuccess("""
                        [
                          {"market":"KRW-BTC","trade_price":123456789.12},
                          {"market":"KRW-ETH","trade_price":4567890.12}
                        ]
                        """, APPLICATION_JSON));

        java.util.List<MarketPrice> prices = new UpbitMarketPriceProvider(builder.build())
                .getCurrentPrices(java.util.List.of("KRW-BTC", "KRW-ETH"));

        assertThat(prices).hasSize(2);
        assertThat(prices.get(0).market()).isEqualTo("KRW-BTC");
        assertThat(prices.get(0).currentPrice()).isEqualByComparingTo("123456789.12");
        assertThat(prices.get(1).market()).isEqualTo("KRW-ETH");
        assertThat(prices.get(1).currentPrice()).isEqualByComparingTo("4567890.12");
        server.verify();
    }

    @Test
    void largeBatchUsesAllKrwTickerEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker/all?quote_currencies=KRW"))
                .andRespond(withSuccess("""
                        [
                          {"market":"KRW-BTC","trade_price":123456789.12},
                          {"market":"KRW-ETH","trade_price":4567890.12}
                        ]
                        """, APPLICATION_JSON));

        java.util.List<String> markets = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> "KRW-COIN" + index)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        markets.set(0, "KRW-BTC");
        markets.set(1, "KRW-ETH");

        java.util.List<MarketPrice> prices = new UpbitMarketPriceProvider(builder.build()).getCurrentPrices(markets);

        assertThat(prices).hasSize(2);
        assertThat(prices.get(0).market()).isEqualTo("KRW-BTC");
        assertThat(prices.get(1).market()).isEqualTo("KRW-ETH");
        server.verify();
    }

    @Test
    void apiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker?markets=KRW-BTC"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new UpbitMarketPriceProvider(builder.build()).getCurrentPrice("KRW-BTC"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Upbit ticker price");
        server.verify();
    }
}
