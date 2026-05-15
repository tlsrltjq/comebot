package com.giseop.comebot.market.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.dto.MarketProviderStatusResponse;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketProviderStatusController {

    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final MarketWebSocketProperties marketWebSocketProperties;
    private final TickerSnapshotStore tickerSnapshotStore;
    private final MarketDataReadinessService marketDataReadinessService;

    public MarketProviderStatusController(
            MarketPriceProviderProperties marketPriceProviderProperties,
            MarketWebSocketProperties marketWebSocketProperties,
            TickerSnapshotStore tickerSnapshotStore,
            MarketDataReadinessService marketDataReadinessService
    ) {
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.marketWebSocketProperties = marketWebSocketProperties;
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.marketDataReadinessService = marketDataReadinessService;
    }

    @GetMapping("/api/market-provider/status")
    public MarketProviderStatusResponse getStatus() {
        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        boolean externalProvider = provider == MarketPriceProviderType.UPBIT
                || provider == MarketPriceProviderType.BINANCE
                || provider == MarketPriceProviderType.SNAPSHOT;
        Instant now = Instant.now();
        int snapshotCount = tickerSnapshotStore.count();
        int freshSnapshotCount = tickerSnapshotStore.countFresh(
                marketWebSocketProperties.orderStaleDuration(),
                now
        );
        int upbitFreshSnapshotCount = tickerSnapshotStore.countFresh(
                ExchangeMode.UPBIT,
                marketWebSocketProperties.orderStaleDuration(),
                now
        );
        int binanceFreshSnapshotCount = tickerSnapshotStore.countFresh(
                ExchangeMode.BINANCE,
                marketWebSocketProperties.orderStaleDuration(),
                now
        );
        MarketDataReadiness upbitReadiness = marketDataReadinessService.readiness(ExchangeMode.UPBIT);
        MarketDataReadiness binanceReadiness = marketDataReadinessService.readiness(ExchangeMode.BINANCE);
        boolean automationReady = upbitReadiness.ready() || binanceReadiness.ready();
        return new MarketProviderStatusResponse(
                provider,
                externalProvider,
                message(provider),
                marketWebSocketProperties.isEnabled(),
                snapshotCount,
                tickerSnapshotStore.count(ExchangeMode.UPBIT),
                tickerSnapshotStore.count(ExchangeMode.BINANCE),
                upbitFreshSnapshotCount,
                binanceFreshSnapshotCount,
                freshSnapshotCount,
                Math.max(0, snapshotCount - freshSnapshotCount),
                marketWebSocketProperties.getOrderStaleMs(),
                automationReady,
                automationReady ? "Market data is ready for at least one exchange" : "Fresh ticker snapshot is not available for any exchange"
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
