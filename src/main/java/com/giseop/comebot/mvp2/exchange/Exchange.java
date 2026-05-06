package com.giseop.comebot.mvp2.exchange;

public enum Exchange {
    UPBIT("Upbit", true),
    BINANCE("Binance", true);

    private final String displayName;
    private final boolean publicMarketDataOnly;

    Exchange(String displayName, boolean publicMarketDataOnly) {
        this.displayName = displayName;
        this.publicMarketDataOnly = publicMarketDataOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPublicMarketDataOnly() {
        return publicMarketDataOnly;
    }
}
