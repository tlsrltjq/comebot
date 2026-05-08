package com.giseop.comebot.market.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.market.service.BinanceUsdtTickerStore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BinanceUsdtTickerPollingSchedulerTest {

    @Test
    void bootstrapFetchesAllUsdtTickers() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/24hr"))
                .andRespond(withSuccess("""
                        [
                          {"symbol":"BTCUSDT","lastPrice":"61234.56","quoteVolume":"300"},
                          {"symbol":"ETHUSDT","lastPrice":"3210.12","quoteVolume":"200"},
                          {"symbol":"ETHBTC","lastPrice":"0.052","quoteVolume":"999"}
                        ]
                        """, APPLICATION_JSON));
        BinanceUsdtTickerPollingProperties properties = new BinanceUsdtTickerPollingProperties();
        properties.setEnabled(true);
        BinanceUsdtTickerStore tickerStore = new BinanceUsdtTickerStore();

        new BinanceUsdtTickerPollingScheduler(
                builder.build(),
                new AtomicBoolean(false),
                tickerStore,
                properties
        ).bootstrap();

        assertThat(tickerStore.latestSymbols()).containsExactly("BTCUSDT", "ETHUSDT");
        server.verify();
    }

    @Test
    void scheduledRefreshDoesNotThrowWhenApiFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.binance.com/api/v3/ticker/24hr"))
                .andRespond(withServerError());
        BinanceUsdtTickerPollingProperties properties = new BinanceUsdtTickerPollingProperties();
        properties.setEnabled(true);
        BinanceUsdtTickerPollingScheduler scheduler = new BinanceUsdtTickerPollingScheduler(
                builder.build(),
                new AtomicBoolean(false),
                new BinanceUsdtTickerStore(),
                properties
        );

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void pollSkipsWhenDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(never(), requestTo("https://api.binance.com/api/v3/ticker/24hr"));

        new BinanceUsdtTickerPollingScheduler(builder.build(), new AtomicBoolean(false)).poll();

        server.verify();
    }

    @Test
    void pollSkipsWhenPreviousRunIsActive() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(never(), requestTo("https://api.binance.com/api/v3/ticker/24hr"));
        BinanceUsdtTickerPollingProperties properties = new BinanceUsdtTickerPollingProperties();
        properties.setEnabled(true);

        new BinanceUsdtTickerPollingScheduler(
                builder.build(),
                new AtomicBoolean(true),
                new BinanceUsdtTickerStore(),
                properties
        ).poll();

        server.verify();
    }

    @Test
    void bootstrapSkipsWhenStartupBootstrapIsDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(never(), requestTo("https://api.binance.com/api/v3/ticker/24hr"));
        BinanceUsdtTickerPollingProperties properties = new BinanceUsdtTickerPollingProperties();
        properties.setEnabled(true);
        properties.setBootstrapOnStartup(false);

        new BinanceUsdtTickerPollingScheduler(
                builder.build(),
                new AtomicBoolean(false),
                new BinanceUsdtTickerStore(),
                properties
        ).bootstrap();

        server.verify();
    }
}
