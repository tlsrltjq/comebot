package com.giseop.comebot.market.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MarketPriceProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    InMemoryMarketPriceProvider.class,
                    UpbitMarketPriceProvider.class,
                    BinanceMarketPriceProvider.class
            );

    @Test
    void defaultProviderIsInMemory() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MarketPriceProvider.class);
            assertThat(context).hasSingleBean(InMemoryMarketPriceProvider.class);
            assertThat(context).doesNotHaveBean(UpbitMarketPriceProvider.class);
            assertThat(context).doesNotHaveBean(BinanceMarketPriceProvider.class);
        });
    }

    @Test
    void upbitProviderIsSelectedWhenConfigured() {
        contextRunner.withPropertyValues("market.price-provider=UPBIT")
                .run(context -> {
                    assertThat(context).hasSingleBean(MarketPriceProvider.class);
                    assertThat(context).hasSingleBean(UpbitMarketPriceProvider.class);
                    assertThat(context).doesNotHaveBean(InMemoryMarketPriceProvider.class);
                    assertThat(context).doesNotHaveBean(BinanceMarketPriceProvider.class);
                });
    }

    @Test
    void binanceProviderIsSelectedWhenConfigured() {
        contextRunner.withPropertyValues("market.price-provider=BINANCE")
                .run(context -> {
                    assertThat(context).hasSingleBean(MarketPriceProvider.class);
                    assertThat(context).hasSingleBean(BinanceMarketPriceProvider.class);
                    assertThat(context).doesNotHaveBean(InMemoryMarketPriceProvider.class);
                    assertThat(context).doesNotHaveBean(UpbitMarketPriceProvider.class);
                });
    }
}
