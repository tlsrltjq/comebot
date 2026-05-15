package com.giseop.comebot.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketDataReadinessServiceTest {

    @Test
    void snapshotWebSocketRequiresFreshSnapshotForExchange() {
        MarketPriceProviderProperties providerProperties = new MarketPriceProviderProperties();
        providerProperties.setPriceProvider(MarketPriceProviderType.SNAPSHOT);
        MarketWebSocketProperties webSocketProperties = new MarketWebSocketProperties();
        webSocketProperties.setEnabled(true);
        webSocketProperties.setUpbitEnabled(true);
        webSocketProperties.setOrderStaleMs(3000);
        TickerSnapshotStore snapshotStore = new TickerSnapshotStore();
        MarketDataReadinessService service = new MarketDataReadinessService(
                providerProperties,
                webSocketProperties,
                snapshotStore
        );

        MarketDataReadiness empty = service.readiness(ExchangeMode.UPBIT);
        snapshotStore.save(snapshot(ExchangeMode.UPBIT, "KRW-BTC", Instant.now()));
        MarketDataReadiness ready = service.readiness(ExchangeMode.UPBIT);

        assertThat(empty.ready()).isFalse();
        assertThat(empty.reason()).contains("Fresh ticker snapshot");
        assertThat(ready.ready()).isTrue();
        assertThat(ready.freshSnapshotCount()).isEqualTo(1);
    }

    @Test
    void nonSnapshotProviderDoesNotRequireSnapshotGuard() {
        MarketPriceProviderProperties providerProperties = new MarketPriceProviderProperties();
        providerProperties.setPriceProvider(MarketPriceProviderType.UPBIT);
        MarketDataReadinessService service = new MarketDataReadinessService(
                providerProperties,
                new MarketWebSocketProperties(),
                new TickerSnapshotStore()
        );

        MarketDataReadiness readiness = service.readiness(ExchangeMode.UPBIT);

        assertThat(readiness.ready()).isTrue();
    }

    private TickerSnapshot snapshot(ExchangeMode exchange, String market, Instant capturedAt) {
        return new TickerSnapshot(
                exchange,
                market,
                new BigDecimal("100"),
                new BigDecimal("1000000"),
                capturedAt,
                PriceSource.WEBSOCKET
        );
    }
}
