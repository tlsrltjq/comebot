package com.giseop.comebot.market.dto;

import com.giseop.comebot.market.provider.MarketPriceProviderType;

public record MarketProviderStatusResponse(
        MarketPriceProviderType provider,
        boolean externalProvider,
        String message
) {
}
