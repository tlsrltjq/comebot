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

        assertThat(service.orderQuantity("KRW-BTC")).isEqualByComparingTo("0.01");
        assertThat(service.orderAmount("KRW-BTC")).isEqualByComparingTo("10000");
        assertThat(service.buyQuantity("KRW-BTC", new BigDecimal("100"))).isEqualByComparingTo("100.00000000");
        assertThat(service.minPriceChangeRate("KRW-BTC")).isEqualByComparingTo("0.3");
        assertThat(service.minTradeAmountChangeRate("KRW-BTC")).isEqualByComparingTo("20");
        assertThat(service.maxPriceChangeRate("KRW-BTC")).isEqualByComparingTo("10");
        assertThat(service.maxHighLowRangeRate("KRW-BTC")).isEqualByComparingTo("20");
    }

    @Test
    void binanceMarketsUseBinanceOrderAmount() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setOrderAmount(new BigDecimal("10000"));
        strategyProperties.setBinanceOrderAmount(new BigDecimal("10"));
        StrategyMarketSettingsService service = new StrategyMarketSettingsService(
                strategyProperties,
                new CandidateScannerProperties(),
                new StrategyMarketOverrideProperties()
        );

        assertThat(service.orderAmount("BTCUSDT")).isEqualByComparingTo("10");
        assertThat(service.buyQuantity("BTCUSDT", new BigDecimal("2"))).isEqualByComparingTo("5.00000000");
    }

    @Test
    void exchangeSpecificScannerThresholdsAreApplied() {
        CandidateScannerProperties scannerProperties = new CandidateScannerProperties();
        CandidateScannerProperties.ExchangeSettings upbit = new CandidateScannerProperties.ExchangeSettings();
        upbit.setMinPriceChangeRate(new BigDecimal("0.15"));
        upbit.setMinTradeAmountChangeRate(new BigDecimal("0"));
        scannerProperties.setUpbit(upbit);
        CandidateScannerProperties.ExchangeSettings binance = new CandidateScannerProperties.ExchangeSettings();
        binance.setMinPriceChangeRate(new BigDecimal("0.8"));
        binance.setMinTradeAmountChangeRate(new BigDecimal("30"));
        scannerProperties.setBinance(binance);

        StrategyMarketSettingsService service = new StrategyMarketSettingsService(
                new StrategyProperties(),
                scannerProperties,
                new StrategyMarketOverrideProperties()
        );

        assertThat(service.minPriceChangeRate("KRW-BTC")).isEqualByComparingTo("0.15");
        assertThat(service.minTradeAmountChangeRate("KRW-BTC")).isEqualByComparingTo("0");
        assertThat(service.minPriceChangeRate("BTCUSDT")).isEqualByComparingTo("0.8");
        assertThat(service.minTradeAmountChangeRate("BTCUSDT")).isEqualByComparingTo("30");
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
