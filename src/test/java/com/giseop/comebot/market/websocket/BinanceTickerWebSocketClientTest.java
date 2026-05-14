package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.dto.BinanceUsdtTickerResponse;
import com.giseop.comebot.market.service.BinanceUsdtTickerStore;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BinanceTickerWebSocketClientTest {

    @Test
    void handleMessageStoresBinanceTickerSnapshot() {
        TickerSnapshotStore store = new TickerSnapshotStore();
        BinanceTickerWebSocketClient client = newClient(store);

        client.handleMessage("""
                {"stream":"btcusdt@ticker","data":{"s":"BTCUSDT","c":"100.5","q":"12345.6"}}
                """);

        assertThat(store.find(ExchangeMode.BINANCE, "BTCUSDT"))
                .hasValueSatisfying(snapshot -> {
                    assertThat(snapshot.tradePrice()).isEqualByComparingTo("100.5");
                    assertThat(snapshot.accTradePrice24h()).isEqualByComparingTo("12345.6");
                    assertThat(snapshot.source()).isEqualTo(PriceSource.WEBSOCKET);
                });
    }

    @Test
    void subscribedMarketsMapsConfiguredKrwMarketsToUsdtSymbols() {
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("KRW-BTC", "ETHUSDT"));
        CandidateSchedulerProperties candidateProperties = new CandidateSchedulerProperties();
        candidateProperties.setMarkets(List.of("KRW-ETH", "BTCUSDT"));

        BinanceTickerWebSocketClient client = new BinanceTickerWebSocketClient(
                new MarketWebSocketProperties(),
                new TickerSnapshotStore(),
                new BinanceUsdtTickerStore(),
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("BTCUSDT", "ETHUSDT");
    }

    @Test
    void subscribedMarketsExpandsAllUsdtFromTickerStore() {
        BinanceUsdtTickerStore tickerStore = new BinanceUsdtTickerStore();
        tickerStore.replace(List.of(
                ticker("BTCUSDT", "100"),
                ticker("XRPUSDT", "300"),
                ticker("ETHUSDT", "200")
        ));
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("ALL_USDT"));
        CandidateSchedulerProperties candidateProperties = new CandidateSchedulerProperties();
        candidateProperties.setMarkets(List.of("ALL_USDT"));

        BinanceTickerWebSocketClient client = new BinanceTickerWebSocketClient(
                new MarketWebSocketProperties(),
                new TickerSnapshotStore(),
                tickerStore,
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("XRPUSDT", "ETHUSDT", "BTCUSDT");
    }

    @Test
    void errorKeepsClientRunningAndSchedulesReconnectWithInitialDelay() {
        ManualReconnectScheduler scheduler = new ManualReconnectScheduler();
        RecordingConnector connector = RecordingConnector.success();
        BinanceTickerWebSocketClient client = reconnectableClient(connector, scheduler, 100, 500);

        client.start();
        client.handleError(new RuntimeException("socket error"));

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
        BinanceTickerWebSocketClient client = reconnectableClient(connector, scheduler, 100, 250);

        client.start();
        scheduler.runLast();
        scheduler.runLast();

        assertThat(client.isRunning()).isTrue();
        assertThat(connector.connectCount()).isEqualTo(3);
        assertThat(scheduler.delays()).containsExactly(100L, 200L, 250L);
        assertThat(client.reconnectDelayMs()).isEqualTo(250);
    }

    private BinanceTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new BinanceTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new BinanceUsdtTickerStore(),
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
        );
    }

    private BinanceTickerWebSocketClient reconnectableClient(
            RecordingConnector connector,
            ManualReconnectScheduler scheduler,
            long initialDelayMs,
            long maxDelayMs
    ) {
        MarketWebSocketProperties properties = new MarketWebSocketProperties();
        properties.setEnabled(true);
        properties.setBinanceEnabled(true);
        properties.setReconnectInitialDelayMs(initialDelayMs);
        properties.setReconnectMaxDelayMs(maxDelayMs);
        TradingSchedulerProperties tradingProperties = new TradingSchedulerProperties();
        tradingProperties.setMarkets(List.of("BTCUSDT"));
        return new BinanceTickerWebSocketClient(
                properties,
                new TickerSnapshotStore(),
                new BinanceUsdtTickerStore(),
                tradingProperties,
                new CandidateSchedulerProperties(),
                connector,
                scheduler
        );
    }

    private BinanceUsdtTickerResponse ticker(String symbol, String quoteVolume) {
        return new BinanceUsdtTickerResponse(symbol, BigDecimal.ONE, new BigDecimal(quoteVolume));
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
