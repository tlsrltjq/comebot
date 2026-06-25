package com.giseop.comebot.market.domain;

import java.util.Locale;

public record MarketIdentity(
        MarketAssetClass assetClass,
        MarketVenue venue,
        String symbol
) {

    public MarketIdentity {
        if (assetClass == null) {
            throw new IllegalArgumentException("assetClass must not be null");
        }
        if (venue == null) {
            throw new IllegalArgumentException("venue must not be null");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
        if (venue.assetClass() != assetClass) {
            throw new IllegalArgumentException("venue does not support assetClass");
        }
        symbol = normalizeSymbol(symbol);
    }

    public static MarketIdentity upbit(String market) {
        return new MarketIdentity(MarketAssetClass.CRYPTO, MarketVenue.UPBIT, market);
    }

    public static MarketIdentity binance(String symbol) {
        return new MarketIdentity(MarketAssetClass.CRYPTO, MarketVenue.BINANCE, symbol);
    }

    public static MarketIdentity usStock(String symbol) {
        return new MarketIdentity(MarketAssetClass.STOCK, MarketVenue.US_STOCK, symbol);
    }

    public String quoteCurrency() {
        return venue.quoteCurrency();
    }

    public String timezone() {
        return venue.timezone();
    }

    public String cacheKey() {
        return assetClass.name() + ":" + venue.name() + ":" + symbol;
    }

    private static String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
