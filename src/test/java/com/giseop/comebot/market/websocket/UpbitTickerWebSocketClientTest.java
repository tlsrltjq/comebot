package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.service.UpbitKrwTickerStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class UpbitTickerWebSocketClientTest {

    @Test
    void handleMessageStoresUpbitTickerSnapshot() {
        TickerSnapshotStore store = new TickerSnapshotStore();
        UpbitTickerWebSocketClient client = newClient(store);

        client.handleMessage("""
                {"code":"KRW-BTC","trade_price":100.5,"acc_trade_price_24h":12345.6}
                """);

        assertThat(store.find(ExchangeMode.UPBIT, "KRW-BTC"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.tradePrice()).isEqualByComparingTo("100.5");
                    assertThat(snapshot.accTradePrice24h()).isEqualByComparingTo("12345.6");
                    assertThat(snapshot.source()).isEqualTo(PriceSource.WEBSOCKET);
                });
    }

    @Test
    void subscribedMarketsUsesConfiguredKrwMarketsOnly() {
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("KRW-BTC", "BTCUSDT"));
        CandidateSchedulerProperties candidateProperties = new CandidateSchedulerProperties();
        candidateProperties.setMarkets(List.of("KRW-ETH", "KRW-BTC"));

        UpbitTickerWebSocketClient client = new UpbitTickerWebSocketClient(
                new MarketWebSocketProperties(),
                new TickerSnapshotStore(),
                new UpbitKrwTickerStore(),
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("KRW-BTC", "KRW-ETH");
    }

    @Test
    void subscribedMarketsExpandsAllKrwFromTickerStore() {
        UpbitKrwTickerStore tickerStore = new UpbitKrwTickerStore();
        tickerStore.replace(List.of(
                ticker("KRW-BTC", "100"),
                ticker("KRW-XRP", "300"),
                ticker("KRW-ETH", "200")
        ));
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("ALL_KRW"));
        CandidateSchedulerProperties candidateProperties = new CandidateSchedulerProperties();
        candidateProperties.setMarkets(List.of("ALL_KRW"));

        UpbitTickerWebSocketClient client = new UpbitTickerWebSocketClient(
                new MarketWebSocketProperties(),
                new TickerSnapshotStore(),
                tickerStore,
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("KRW-XRP", "KRW-ETH", "KRW-BTC");
    }

    @Test
    void closeKeepsClientRunningAndSchedulesReconnectWithInitialDelay() {
        ManualReconnectScheduler scheduler = new ManualReconnectScheduler();
        RecordingConnector connector = RecordingConnector.success();
        UpbitTickerWebSocketClient client = reconnectableClient(connector, scheduler, 100, 500);

        client.start();
        client.handleClose(1006, "abnormal");

        assertThat(client.isRunning()).isTrue();
        assertThat(connector.connectCount()).isEqualTo(1);
        assertThat(scheduler.delays()).containsExactly(100L);

        scheduler.runLast();

        assertThat(connector.connectCount()).isEqualTo(2);
        assertThat(client.reconnectDelayMs()).isEqualTo(100);
    }

    @Test
    void repeatedConnectionFailuresUseBackoffUntilMaxDelay() {
        ManualReconnectScheduler scheduler = new ManualReconnectScheduler();
        RecordingConnector connector = RecordingConnector.failure();
        UpbitTickerWebSocketClient client = reconnectableClient(connector, scheduler, 100, 250);

        client.start();
        scheduler.runLast();
        scheduler.runLast();

        assertThat(client.isRunning()).isTrue();
        assertThat(connector.connectCount()).isEqualTo(3);
        assertThat(scheduler.delays()).containsExactly(100L, 200L, 250L);
        assertThat(client.reconnectDelayMs()).isEqualTo(250);
    }

    @Test
    void stopPreventsScheduledReconnectFromConnectingAgain() {
        ManualReconnectScheduler scheduler = new ManualReconnectScheduler();
        RecordingConnector connector = RecordingConnector.success();
        UpbitTickerWebSocketClient client = reconnectableClient(connector, scheduler, 100, 500);

        client.start();
        client.handleError(new RuntimeException("socket error"));
        client.stop();
        scheduler.runLast();

        assertThat(client.isRunning()).isFalse();
        assertThat(connector.connectCount()).isEqualTo(1);
    }

    private UpbitTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new UpbitTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new UpbitKrwTickerStore(),
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
        );
    }

    private UpbitTickerWebSocketClient reconnectableClient(
            RecordingConnector connector,
            ManualReconnectScheduler scheduler,
            long initialDelayMs,
            long maxDelayMs
    ) {
        MarketWebSocketProperties properties = new MarketWebSocketProperties();
        properties.setEnabled(true);
        properties.setUpbitEnabled(true);
        properties.setReconnectInitialDelayMs(initialDelayMs);
        properties.setReconnectMaxDelayMs(maxDelayMs);
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("KRW-BTC"));
        return new UpbitTickerWebSocketClient(
                properties,
                new TickerSnapshotStore(),
                new UpbitKrwTickerStore(),
                tradingProperties,
                new CandidateSchedulerProperties(),
                connector,
                scheduler
        );
    }

    private UpbitKrwTickerResponse ticker(String market, String tradeAmount24h) {
        return new UpbitKrwTickerResponse(
                market,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                new BigDecimal(tradeAmount24h)
        );
    }

    private static final class ManualReconnectScheduler implements ReconnectScheduler {

        private final List<Long> delays = new ArrayList<>();
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void schedule(Runnable task, long delayMs) {
            delays.add(delayMs);
            tasks.add(task);
        }

        List<Long> delays() {
            return delays;
        }

        void runLast() {
            tasks.get(tasks.size() - 1).run();
        }
    }

    private static final class RecordingConnector implements WebSocketConnector {

        private final boolean fail;
        private final List<URI> uris = new ArrayList<>();

        private RecordingConnector(boolean fail) {
            this.fail = fail;
        }

        static RecordingConnector success() {
            return new RecordingConnector(false);
        }

        static RecordingConnector failure() {
            return new RecordingConnector(true);
        }

        @Override
        public CompletableFuture<WebSocket> connect(URI uri, WebSocket.Listener listener) {
            uris.add(uri);
            if (fail) {
                return CompletableFuture.failedFuture(new RuntimeException("connect failed"));
            }
            return CompletableFuture.completedFuture(mock(WebSocket.class));
        }

        int connectCount() {
            return uris.size();
        }
    }
}
