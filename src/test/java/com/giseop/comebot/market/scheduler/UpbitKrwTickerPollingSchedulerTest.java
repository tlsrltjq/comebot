package com.giseop.comebot.market.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.giseop.comebot.market.service.UpbitKrwTickerStore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class UpbitKrwTickerPollingSchedulerTest {

    @Test
    void pollFetchesAllKrwTickers() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker/all?quote_currencies=KRW"))
                .andRespond(withSuccess("""
                        [
                          {"market":"KRW-BTC","trade_price":113000000},
                          {"market":"KRW-ETH","trade_price":3360000}
                        ]
                        """, APPLICATION_JSON));

        UpbitKrwTickerStore tickerStore = new UpbitKrwTickerStore();
        new UpbitKrwTickerPollingScheduler(builder.build(), new AtomicBoolean(false), tickerStore).poll();

        assertThat(tickerStore.latestMarkets()).containsExactly("KRW-BTC", "KRW-ETH");
        server.verify();
    }

    @Test
    void pollDoesNotThrowWhenApiFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://api.upbit.com/v1/ticker/all?quote_currencies=KRW"))
                .andRespond(withServerError());

        UpbitKrwTickerPollingScheduler scheduler = new UpbitKrwTickerPollingScheduler(builder.build(), new AtomicBoolean(false));

        assertThatCode(scheduler::poll).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void pollSkipsWhenPreviousRunIsActive() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.upbit.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(never(), requestTo("https://api.upbit.com/v1/ticker/all?quote_currencies=KRW"));

        new UpbitKrwTickerPollingScheduler(builder.build(), new AtomicBoolean(true)).poll();

        server.verify();
    }
}
