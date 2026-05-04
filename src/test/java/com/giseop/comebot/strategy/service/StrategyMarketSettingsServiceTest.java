package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyMarketSettingsServiceTest {

    @Test
    void returnsDefaultValuesWhenMarketOverrideDoesNotExist() {
        StrategyProperties strategyProperties = new StrategyProperties();
        CandidateScannerProperties scannerProperties = new CandidateScannerProperties();
        StrategyMarketSettingsService service = new StrategyMarketSettingsService(
                strategyProperties,
                scannerProperties,
                new StrategyMarketOverrideProperties()
        );

        assertThat(service.orderQuantity("KRW-BTC")).isEqualByComparingTo("0.001");
        assertThat(service.buyQuantity("KRW-BTC", new BigDecimal("100"))).isEqualByComparingTo("100.00000000");
        assertThat(service.minPriceChangeRate("KRW-BTC")).isEqualByComparingTo("0.3");
        assertThat(service.minTradeAmountChangeRate("KRW-BTC")).isEqualByComparingTo("20");
        assertThat(service.maxPriceChangeRate("KRW-BTC")).isEqualByComparingTo("10");
        assertThat(service.maxHighLowRangeRate("KRW-BTC")).isEqualByComparingTo("20");
    }

    @Test
    void returnsMarketOverrideValuesWhenConfigured() {
        StrategyMarketOverrideProperties.MarketOverride override = new StrategyMarketOverrideProperties.MarketOverride();
        override.setOrderQuantity(new BigDecimal("0.02"));
        override.setMinPriceChangeRate(new BigDecimal("3"));
        override.setMinTradeAmountChangeRate(new BigDecimal("5"));
        override.setMaxPriceChangeRate(new BigDecimal("12"));
        override.setMaxHighLowRangeRate(new BigDecimal("20"));
        StrategyMarketOverrideProperties overrideProperties = new StrategyMarketOverrideProperties();
        overrideProperties.setMarkets(Map.of("KRW-BTC", override));

        StrategyMarketSettingsService service = new StrategyMarketSettingsService(
                new StrategyProperties(),
                new CandidateScannerProperties(),
                overrideProperties
        );

        assertThat(service.orderQuantity("KRW-BTC")).isEqualByComparingTo("0.02");
        assertThat(service.minPriceChangeRate("KRW-BTC")).isEqualByComparingTo("3");
        assertThat(service.minTradeAmountChangeRate("KRW-BTC")).isEqualByComparingTo("5");
        assertThat(service.maxPriceChangeRate("KRW-BTC")).isEqualByComparingTo("12");
        assertThat(service.maxHighLowRangeRate("KRW-BTC")).isEqualByComparingTo("20");
    }
}
