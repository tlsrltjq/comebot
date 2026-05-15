package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class MarketDataReadinessService {

    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final MarketWebSocketProperties marketWebSocketProperties;
    private final TickerSnapshotStore tickerSnapshotStore;

    public MarketDataReadinessService(
            MarketPriceProviderProperties marketPriceProviderProperties,
            MarketWebSocketProperties marketWebSocketProperties,
            TickerSnapshotStore tickerSnapshotStore
    ) {
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.marketWebSocketProperties = marketWebSocketProperties;
        this.tickerSnapshotStore = tickerSnapshotStore;
    }

    public MarketDataReadiness readiness(ExchangeMode exchange) {
        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        if (provider != MarketPriceProviderType.SNAPSHOT || !marketWebSocketProperties.isEnabled()) {
            return MarketDataReadiness.ready(exchange, "Snapshot readiness guard is not required for this provider");
        }
        if (!isWebSocketEnabled(exchange)) {
            return MarketDataReadiness.ready(exchange, "WebSocket is disabled for this exchange");
        }
        Instant now = Instant.now();
        return MarketDataReadiness.snapshot(
                exchange,
                tickerSnapshotStore.count(exchange),
                tickerSnapshotStore.countFresh(exchange, marketWebSocketProperties.orderStaleDuration(), now)
        );
    }

    public boolean isReady(ExchangeMode exchange) {
        return readiness(exchange).ready();
    }

    private boolean isWebSocketEnabled(ExchangeMode exchange) {
        if (exchange == ExchangeMode.BINANCE) {
            return marketWebSocketProperties.isBinanceEnabled();
        }
        return marketWebSocketProperties.isUpbitEnabled();
    }
}
