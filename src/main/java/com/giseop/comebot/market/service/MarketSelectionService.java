package com.giseop.comebot.market.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarketSelectionService {

    public static final String ALL_KRW = "ALL_KRW";
    private static final int DEFAULT_TOP_KRW_MARKET_LIMIT = 50;

    private final UpbitKrwTickerStore upbitKrwTickerStore;

    public MarketSelectionService(UpbitKrwTickerStore upbitKrwTickerStore) {
        this.upbitKrwTickerStore = upbitKrwTickerStore;
    }

    public List<String> resolve(List<String> configuredMarkets) {
        List<String> markets = normalize(configuredMarkets);
        if (markets.contains(ALL_KRW)) {
            return upbitKrwTickerStore.topMarkets(DEFAULT_TOP_KRW_MARKET_LIMIT);
        }
        return markets;
    }

    public boolean isAllowed(String market, List<String> configuredMarkets) {
        if (market == null || market.isBlank()) {
            return false;
        }
        List<String> markets = normalize(configuredMarkets);
        if (markets.contains(ALL_KRW)) {
            return market.startsWith("KRW-");
        }
        return markets.contains(market);
    }

    private List<String> normalize(List<String> configuredMarkets) {
        if (configuredMarkets == null) {
            return List.of();
        }
        return configuredMarkets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
