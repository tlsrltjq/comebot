package com.giseop.comebot.market.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.dto.BinanceUsdtTickerResponse;
import com.giseop.comebot.market.service.BinanceUsdtTickerStore;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import java.math.BigDecimal;
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

    private BinanceTickerWebSocketClient newClient(TickerSnapshotStore store) {
        return new BinanceTickerWebSocketClient(
                new MarketWebSocketProperties(),
                store,
                new BinanceUsdtTickerStore(),
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties()
        );
    }

    private BinanceUsdtTickerResponse ticker(String symbol, String quoteVolume) {
        return new BinanceUsdtTickerResponse(symbol, BigDecimal.ONE, new BigDecimal(quoteVolume));
    }
}
