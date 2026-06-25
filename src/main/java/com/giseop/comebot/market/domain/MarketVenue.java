package com.giseop.comebot.market.domain;

public enum MarketVenue {
    UPBIT(MarketAssetClass.CRYPTO, "KRW", "Asia/Seoul"),
    BINANCE(MarketAssetClass.CRYPTO, "USDT", "UTC"),
    US_STOCK(MarketAssetClass.STOCK, "USD", "America/New_York");

    private final MarketAssetClass assetClass;
    private final String quoteCurrency;
    private final String timezone;

    MarketVenue(MarketAssetClass assetClass, String quoteCurrency, String timezone) {
        this.assetClass = assetClass;
        this.quoteCurrency = quoteCurrency;
        this.timezone = timezone;
    }

    public MarketAssetClass assetClass() {
        return assetClass;
    }

    public String quoteCurrency() {
        return quoteCurrency;
    }

    public String timezone() {
        return timezone;
    }
}
