package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;
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

    public List<String> resolve(ExchangeMode exchange, List<String> configuredMarkets) {
        ExchangeMode mode = exchange == null ? ExchangeMode.UPBIT : exchange;
        List<String> markets = normalize(configuredMarkets);
        if (mode == ExchangeMode.UPBIT && markets.contains(ALL_KRW)) {
            return upbitKrwTickerStore.topMarkets(DEFAULT_TOP_KRW_MARKET_LIMIT);
        }
        if (mode == ExchangeMode.BINANCE && markets.contains(ALL_USDT)) {
            return binanceUsdtTickerStore.topSymbols(DEFAULT_TOP_USDT_SYMBOL_LIMIT);
        }
        return markets.stream()
                .filter(market -> isMarketForExchange(mode, market))
                .toList();
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
