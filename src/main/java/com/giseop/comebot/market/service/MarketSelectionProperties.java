package com.giseop.comebot.market.service;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market.selection")
public class MarketSelectionProperties {

    private int topKrwMarketLimit = 20;
    private int topUsdtSymbolLimit = 30;
    private List<String> excludedMarkets = List.of();

    public int getTopKrwMarketLimit() {
        return topKrwMarketLimit;
    }

    public void setTopKrwMarketLimit(int topKrwMarketLimit) {
        this.topKrwMarketLimit = topKrwMarketLimit <= 0 ? 20 : topKrwMarketLimit;
    }

    public int getTopUsdtSymbolLimit() {
        return topUsdtSymbolLimit;
    }

    public void setTopUsdtSymbolLimit(int topUsdtSymbolLimit) {
        this.topUsdtSymbolLimit = topUsdtSymbolLimit <= 0 ? 30 : topUsdtSymbolLimit;
    }

    public List<String> getExcludedMarkets() {
        return excludedMarkets;
    }

    public void setExcludedMarkets(List<String> excludedMarkets) {
        this.excludedMarkets = excludedMarkets == null ? List.of() : excludedMarkets;
    }
}
