package com.giseop.comebot.market.dto;

import com.giseop.comebot.market.provider.MarketPriceProviderType;

public record MarketProviderStatusResponse(
        MarketPriceProviderType provider,
        boolean externalProvider,
        String message,
        boolean webSocketEnabled,
        int snapshotCount,
        int upbitSnapshotCount,
        int binanceSnapshotCount,
        int freshSnapshotCount,
        int staleSnapshotCount,
        long orderStaleMs
) {

    public MarketProviderStatusResponse(
            MarketPriceProviderType provider,
            boolean externalProvider,
            String message
    ) {
        this(provider, externalProvider, message, false, 0, 0, 0, 0, 0, 0);
    }
}
