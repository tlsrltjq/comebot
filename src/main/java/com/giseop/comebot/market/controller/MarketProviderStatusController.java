package com.giseop.comebot.market.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.dto.MarketProviderStatusResponse;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketProviderStatusController {

    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final MarketWebSocketProperties marketWebSocketProperties;
    private final TickerSnapshotStore tickerSnapshotStore;

    public MarketProviderStatusController(
            MarketPriceProviderProperties marketPriceProviderProperties,
            MarketWebSocketProperties marketWebSocketProperties,
            TickerSnapshotStore tickerSnapshotStore
    ) {
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.marketWebSocketProperties = marketWebSocketProperties;
        this.tickerSnapshotStore = tickerSnapshotStore;
    }

    @GetMapping("/api/market-provider/status")
    public MarketProviderStatusResponse getStatus() {
        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        boolean externalProvider = provider == MarketPriceProviderType.UPBIT
                || provider == MarketPriceProviderType.BINANCE
                || provider == MarketPriceProviderType.SNAPSHOT;
        return new MarketProviderStatusResponse(
                provider,
                externalProvider,
                message(provider),
                marketWebSocketProperties.isEnabled(),
                tickerSnapshotStore.count(),
                tickerSnapshotStore.count(ExchangeMode.UPBIT),
                tickerSnapshotStore.count(ExchangeMode.BINANCE)
        );
    }

    private String message(MarketPriceProviderType provider) {
        if (provider == MarketPriceProviderType.UPBIT) {
            return "Using Upbit public ticker API for market prices. Orders remain PAPER_TRADING only.";
        }
        if (provider == MarketPriceProviderType.BINANCE) {
            return "Using Binance public spot ticker API for market prices. Orders remain PAPER_TRADING only.";
        }
        if (provider == MarketPriceProviderType.SNAPSHOT) {
            return "Using WebSocket ticker snapshots first with REST fallback. Orders remain PAPER_TRADING only.";
        }
        return "Using in-memory test market prices.";
    }
}
