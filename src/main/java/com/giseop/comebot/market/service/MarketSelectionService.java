package com.giseop.comebot.market.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketSelectionService {

    public static final String ALL_KRW = "ALL_KRW";
    public static final String ALL_USDT = "ALL_USDT";
    private static final int DEFAULT_TOP_KRW_MARKET_LIMIT = 50;
    private static final int DEFAULT_TOP_USDT_SYMBOL_LIMIT = 50;

    private final UpbitKrwTickerStore upbitKrwTickerStore;
    private final BinanceUsdtTickerStore binanceUsdtTickerStore;

    public MarketSelectionService(UpbitKrwTickerStore upbitKrwTickerStore) {
        this(upbitKrwTickerStore, new BinanceUsdtTickerStore());
    }

    @Autowired
    public MarketSelectionService(
            UpbitKrwTickerStore upbitKrwTickerStore,
            BinanceUsdtTickerStore binanceUsdtTickerStore
    ) {
        this.upbitKrwTickerStore = upbitKrwTickerStore;
        this.binanceUsdtTickerStore = binanceUsdtTickerStore;
    }

    public List<String> resolve(List<String> configuredMarkets) {
        List<String> markets = normalize(configuredMarkets);
        if (markets.contains(ALL_KRW)) {
            return upbitKrwTickerStore.topMarkets(DEFAULT_TOP_KRW_MARKET_LIMIT);
        }
        if (markets.contains(ALL_USDT)) {
            return binanceUsdtTickerStore.topSymbols(DEFAULT_TOP_USDT_SYMBOL_LIMIT);
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
        if (markets.contains(ALL_USDT)) {
            return market.endsWith("USDT");
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
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }
}
