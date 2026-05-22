package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketSelectionService {

    public static final String ALL_KRW = "ALL_KRW";
    public static final String ALL_USDT = "ALL_USDT";

    private final UpbitKrwTickerStore upbitKrwTickerStore;
    private final BinanceUsdtTickerStore binanceUsdtTickerStore;
    private final MarketSelectionProperties marketSelectionProperties;

    public MarketSelectionService(UpbitKrwTickerStore upbitKrwTickerStore) {
        this(upbitKrwTickerStore, new BinanceUsdtTickerStore(), new MarketSelectionProperties());
    }

    public MarketSelectionService(
            UpbitKrwTickerStore upbitKrwTickerStore,
            BinanceUsdtTickerStore binanceUsdtTickerStore
    ) {
        this(upbitKrwTickerStore, binanceUsdtTickerStore, new MarketSelectionProperties());
    }

    @Autowired
    public MarketSelectionService(
            UpbitKrwTickerStore upbitKrwTickerStore,
            BinanceUsdtTickerStore binanceUsdtTickerStore,
            MarketSelectionProperties marketSelectionProperties
    ) {
        this.upbitKrwTickerStore = upbitKrwTickerStore;
        this.binanceUsdtTickerStore = binanceUsdtTickerStore;
        this.marketSelectionProperties = marketSelectionProperties;
    }

    public List<String> resolve(List<String> configuredMarkets) {
        List<String> markets = normalize(configuredMarkets);
        if (markets.contains(ALL_KRW)) {
            return exclude(upbitKrwTickerStore.topMarkets(marketSelectionProperties.getTopKrwMarketLimit()));
        }
        if (markets.contains(ALL_USDT)) {
            return exclude(binanceUsdtTickerStore.topSymbols(marketSelectionProperties.getTopUsdtSymbolLimit()));
        }
        return exclude(markets);
    }

    public List<String> resolve(ExchangeMode exchange, List<String> configuredMarkets) {
        ExchangeMode mode = exchange == null ? ExchangeMode.UPBIT : exchange;
        List<String> markets = normalize(configuredMarkets);
        if (mode == ExchangeMode.UPBIT && markets.contains(ALL_KRW)) {
            return exclude(upbitKrwTickerStore.topMarkets(marketSelectionProperties.getTopKrwMarketLimit()));
        }
        if (mode == ExchangeMode.BINANCE && markets.contains(ALL_USDT)) {
            return exclude(binanceUsdtTickerStore.topSymbols(marketSelectionProperties.getTopUsdtSymbolLimit()));
        }
        return exclude(markets.stream()
                .filter(market -> isMarketForExchange(mode, market))
                .toList());
    }

    public boolean isAllowed(String market, List<String> configuredMarkets) {
        if (market == null || market.isBlank()) {
            return false;
        }
        if (isExcluded(market)) {
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

    private List<String> exclude(List<String> markets) {
        if (marketSelectionProperties.getExcludedMarkets().isEmpty()) {
            return markets;
        }
        return markets.stream().filter(m -> !isExcluded(m)).toList();
    }

    private boolean isExcluded(String market) {
        return marketSelectionProperties.getExcludedMarkets().stream()
                .anyMatch(ex -> ex.equalsIgnoreCase(market));
    }

    private boolean isMarketForExchange(ExchangeMode exchange, String market) {
        if (market == null || market.isBlank() || ALL_KRW.equals(market) || ALL_USDT.equals(market)) {
            return false;
        }
        if (exchange == ExchangeMode.BINANCE) {
            return market.endsWith("USDT") && !market.startsWith("KRW-");
        }
        return market.startsWith("KRW-");
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
