package com.giseop.comebot.market.controller;

import com.giseop.comebot.market.dto.MarketProviderStatusResponse;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketProviderStatusController {

    private final MarketPriceProviderProperties marketPriceProviderProperties;

    public MarketProviderStatusController(MarketPriceProviderProperties marketPriceProviderProperties) {
        this.marketPriceProviderProperties = marketPriceProviderProperties;
    }

    @GetMapping("/api/market-provider/status")
    public MarketProviderStatusResponse getStatus() {
        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        boolean externalProvider = provider == MarketPriceProviderType.UPBIT
                || provider == MarketPriceProviderType.BINANCE;
        return new MarketProviderStatusResponse(
                provider,
                externalProvider,
                message(provider)
        );
    }

    private String message(MarketPriceProviderType provider) {
        if (provider == MarketPriceProviderType.UPBIT) {
            return "Using Upbit public ticker API for market prices. Orders remain PAPER_TRADING only.";
        }
        if (provider == MarketPriceProviderType.BINANCE) {
            return "Using Binance public spot ticker API for market prices. Orders remain PAPER_TRADING only.";
        }
        return "Using in-memory test market prices.";
    }
}
