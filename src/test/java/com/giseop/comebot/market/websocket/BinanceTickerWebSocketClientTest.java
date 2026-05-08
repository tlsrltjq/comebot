package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.util.List;
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
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("BTCUSDT", "ETHUSDT");
    }

    private BinanceTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new BinanceTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
        );
    }
}
