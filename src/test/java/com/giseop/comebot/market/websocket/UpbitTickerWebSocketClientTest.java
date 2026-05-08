package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.util.List;
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
                tradingProperties,
                candidateProperties
        );

        assertThat(client.subscribedMarkets()).containsExactly("KRW-BTC", "KRW-ETH");
    }

    private UpbitTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new UpbitTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
        );
    }
}
