package com.giseop.comebot.market.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MarketIdentityTest {

    @Test
    void createsCryptoIdentitiesWithoutMixingVenues() {
        MarketIdentity upbit = MarketIdentity.upbit("krw-btc");
        MarketIdentity binance = MarketIdentity.binance("btcusdt");

        assertThat(upbit.assetClass()).isEqualTo(MarketAssetClass.CRYPTO);
        assertThat(upbit.venue()).isEqualTo(MarketVenue.UPBIT);
        assertThat(upbit.symbol()).isEqualTo("KRW-BTC");
        assertThat(upbit.quoteCurrency()).isEqualTo("KRW");

        assertThat(binance.assetClass()).isEqualTo(MarketAssetClass.CRYPTO);
        assertThat(binance.venue()).isEqualTo(MarketVenue.BINANCE);
        assertThat(binance.symbol()).isEqualTo("BTCUSDT");
        assertThat(binance.quoteCurrency()).isEqualTo("USDT");
    }

    @Test
    void createsUsStockIdentityWithStockMetadata() {
        MarketIdentity apple = MarketIdentity.usStock("aapl");

        assertThat(apple.assetClass()).isEqualTo(MarketAssetClass.STOCK);
        assertThat(apple.venue()).isEqualTo(MarketVenue.US_STOCK);
        assertThat(apple.symbol()).isEqualTo("AAPL");
        assertThat(apple.quoteCurrency()).isEqualTo("USD");
        assertThat(apple.timezone()).isEqualTo("America/New_York");
        assertThat(apple.cacheKey()).isEqualTo("STOCK:US_STOCK:AAPL");
    }

    @Test
    void rejectsVenueAndAssetClassMismatch() {
        assertThatThrownBy(() -> new MarketIdentity(MarketAssetClass.STOCK, MarketVenue.BINANCE, "AAPL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("venue does not support assetClass");
    }

    @Test
    void rejectsBlankSymbol() {
        assertThatThrownBy(() -> MarketIdentity.usStock(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("symbol must not be blank");
    }
}
