package com.giseop.comebot.market.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market")
public class MarketPriceProviderProperties {

    private MarketPriceProviderType priceProvider = MarketPriceProviderType.IN_MEMORY;

    public MarketPriceProviderType getPriceProvider() {
        return priceProvider;
    }

    public void setPriceProvider(MarketPriceProviderType priceProvider) {
        this.priceProvider = priceProvider == null ? MarketPriceProviderType.IN_MEMORY : priceProvider;
    }
}
