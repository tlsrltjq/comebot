package com.giseop.comebot.market.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.market.domain.MarketPrice;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InMemoryMarketPriceProviderTest {

    @Test
    void updatePriceChangesCurrentPriceForTests() {
        InMemoryMarketPriceProvider provider = new InMemoryMarketPriceProvider();

        provider.updatePrice("KRW-BTC", new BigDecimal("12345"));

        MarketPrice price = provider.getCurrentPrice("KRW-BTC");

        assertThat(price.market()).isEqualTo("KRW-BTC");
        assertThat(price.currentPrice()).isEqualByComparingTo("12345");
        assertThat(price.capturedAt()).isNotNull();
    }
}
