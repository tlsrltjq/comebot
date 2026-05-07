package com.giseop.comebot.market.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.market.domain.MarketPrice;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BinanceMarketPriceProviderTest {

    @Test
    void tickerPriceResponseIsMappedToCurrentPrice() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withSuccess("{\"symbol\":\"BTCUSDT\",\"price\":\"61234.56\"}", APPLICATION_JSON));

        MarketPrice price = new BinanceMarketPriceProvider(builder.build()).getCurrentPrice("btcusdt");

        assertThat(price.market()).isEqualTo("BTCUSDT");
        assertThat(price.currentPrice()).isEqualByComparingTo("61234.56");
        assertThat(price.capturedAt()).isNotNull();
        server.verify();
    }

    @Test
    void multipleSymbolsAreDeduplicatedAndMapped() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withSuccess("{\"symbol\":\"BTCUSDT\",\"price\":\"61234.56\"}", APPLICATION_JSON));
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=ETHUSDT"))
                .andRespond(withSuccess("{\"symbol\":\"ETHUSDT\",\"price\":\"3210.12\"}", APPLICATION_JSON));

        List<MarketPrice> prices = new BinanceMarketPriceProvider(builder.build())
                .getCurrentPrices(List.of("BTCUSDT", "btcusdt", "ETHUSDT"));

        assertThat(prices).hasSize(2);
        assertThat(prices.get(0).market()).isEqualTo("BTCUSDT");
        assertThat(prices.get(0).currentPrice()).isEqualByComparingTo("61234.56");
        assertThat(prices.get(1).market()).isEqualTo("ETHUSDT");
        assertThat(prices.get(1).currentPrice()).isEqualByComparingTo("3210.12");
        server.verify();
    }

    @Test
    void apiFailureThrowsClearException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new BinanceMarketPriceProvider(builder.build()).getCurrentPrice("BTCUSDT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to fetch Binance ticker price");
        server.verify();
    }

    @Test
    void nonUsdtSymbolIsRejected() {
        BinanceMarketPriceProvider provider = new BinanceMarketPriceProvider(RestClient.builder().build());

        assertThatThrownBy(() -> provider.getCurrentPrice("BTCBUSD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only Binance USDT spot symbols are supported");
    }
}
