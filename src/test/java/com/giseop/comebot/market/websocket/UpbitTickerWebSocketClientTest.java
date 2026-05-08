package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.dto.UpbitKrwTickerResponse;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.service.UpbitKrwTickerStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
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

    private UpbitTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new UpbitTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new UpbitKrwTickerStore(),
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
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
}
