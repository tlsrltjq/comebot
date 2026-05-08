package com.giseop.comebot.market.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SnapshotMarketPriceProviderTest {

    @Test
    void currentPriceUsesFreshSnapshotBeforeRestFallback() {
        TickerSnapshotStore store = new TickerSnapshotStore();
        MarketWebSocketProperties properties = new MarketWebSocketProperties();
        AtomicInteger fallbackCalls = new AtomicInteger();
        store.save(new TickerSnapshot(
                ExchangeMode.UPBIT,
                "KRW-BTC",
                new BigDecimal("100"),
                null,
                Instant.now(),
                PriceSource.WEBSOCKET
        ));

        SnapshotMarketPriceProvider provider = new SnapshotMarketPriceProvider(
                store,
                properties,
                market -> {
                    fallbackCalls.incrementAndGet();
                    return price(market, "200");
                },
                market -> price(market, "300")
        );

        MarketPrice price = provider.getCurrentPrice("krw-btc");

        assertThat(price.currentPrice()).isEqualByComparingTo("100");
        assertThat(fallbackCalls).hasValue(0);
    }

    @Test
    void currentPriceFallsBackToRestWhenSnapshotIsStale() {
        TickerSnapshotStore store = new TickerSnapshotStore();
        MarketWebSocketProperties properties = new MarketWebSocketProperties();
        properties.setOrderStaleMs(1);
        store.save(new TickerSnapshot(
                ExchangeMode.UPBIT,
                "KRW-BTC",
                new BigDecimal("100"),
                null,
                Instant.now().minusSeconds(1),
                PriceSource.WEBSOCKET
        ));

        SnapshotMarketPriceProvider provider = new SnapshotMarketPriceProvider(
                store,
                properties,
                market -> price(market, "200"),
                market -> price(market, "300")
        );

        MarketPrice price = provider.getCurrentPrice("KRW-BTC");

        assertThat(price.currentPrice()).isEqualByComparingTo("200");
        assertThat(store.find(ExchangeMode.UPBIT, "KRW-BTC"))
                .hasValueSatisfying(snapshot -> assertThat(snapshot.source()).isEqualTo(PriceSource.REST_FALLBACK));
    }

    @Test
    void currentPriceRoutesBinanceSymbolsToBinanceFallback() {
        TickerSnapshotStore store = new TickerSnapshotStore();
        MarketWebSocketProperties properties = new MarketWebSocketProperties();

        SnapshotMarketPriceProvider provider = new SnapshotMarketPriceProvider(
                store,
                properties,
                market -> price(market, "200"),
                market -> price(market, "300")
        );

        MarketPrice price = provider.getCurrentPrice("btcusdt");

        assertThat(price.market()).isEqualTo("BTCUSDT");
        assertThat(price.currentPrice()).isEqualByComparingTo("300");
    }

    @Test
    void currentPriceThrowsWhenFreshSnapshotAndFallbackAreUnavailable() {
        SnapshotMarketPriceProvider provider = new SnapshotMarketPriceProvider(
                new TickerSnapshotStore(),
                new MarketWebSocketProperties(),
                market -> {
                    throw new IllegalStateException("upbit unavailable");
                },
                market -> price(market, "300")
        );

        assertThatThrownBy(() -> provider.getCurrentPrice("KRW-BTC"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REST fallback failed");
    }

    private MarketPrice price(String market, String price) {
        return new MarketPrice(market.toUpperCase(), new BigDecimal(price), Instant.now());
    }
}
