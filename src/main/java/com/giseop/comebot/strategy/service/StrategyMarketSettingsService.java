package com.giseop.comebot.strategy.service;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class StrategyMarketSettingsService {

    private static final int QUANTITY_SCALE = 8;

    private final StrategyProperties strategyProperties;
    private final CandidateScannerProperties candidateScannerProperties;
    private final StrategyMarketOverrideProperties strategyMarketOverrideProperties;

    public StrategyMarketSettingsService(
            StrategyProperties strategyProperties,
            CandidateScannerProperties candidateScannerProperties,
            StrategyMarketOverrideProperties strategyMarketOverrideProperties
    ) {
        this.strategyProperties = strategyProperties;
        this.candidateScannerProperties = candidateScannerProperties;
        this.strategyMarketOverrideProperties = strategyMarketOverrideProperties;
    }

    public BigDecimal orderQuantity(String market) {
        StrategyMarketOverrideProperties.MarketOverride override = strategyMarketOverrideProperties.get(market);
        return override != null && override.getOrderQuantity() != null
                ? override.getOrderQuantity()
                : strategyProperties.getOrderQuantity();
    }

    public BigDecimal buyQuantity(String market, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return orderQuantity(market);
        }
        return orderAmount(market).divide(price, QUANTITY_SCALE, RoundingMode.DOWN);
    }

    public BigDecimal orderAmount(String market) {
        return strategyProperties.getOrderAmount(exchangeOf(market));
    }

    public BigDecimal minPriceChangeRate(String market) {
        StrategyMarketOverrideProperties.MarketOverride override = strategyMarketOverrideProperties.get(market);
        return override != null && override.getMinPriceChangeRate() != null
                ? override.getMinPriceChangeRate()
                : candidateScannerProperties.getMinPriceChangeRate();
    }

    public BigDecimal minTradeAmountChangeRate(String market) {
        StrategyMarketOverrideProperties.MarketOverride override = strategyMarketOverrideProperties.get(market);
        return override != null && override.getMinTradeAmountChangeRate() != null
                ? override.getMinTradeAmountChangeRate()
                : candidateScannerProperties.getMinTradeAmountChangeRate();
    }

    public BigDecimal maxPriceChangeRate(String market) {
        StrategyMarketOverrideProperties.MarketOverride override = strategyMarketOverrideProperties.get(market);
        return override != null && override.getMaxPriceChangeRate() != null
                ? override.getMaxPriceChangeRate()
                : candidateScannerProperties.getMaxPriceChangeRate();
    }

    public BigDecimal maxHighLowRangeRate(String market) {
        StrategyMarketOverrideProperties.MarketOverride override = strategyMarketOverrideProperties.get(market);
        return override != null && override.getMaxHighLowRangeRate() != null
                ? override.getMaxHighLowRangeRate()
                : candidateScannerProperties.getMaxHighLowRangeRate();
    }

    private ExchangeMode exchangeOf(String market) {
        if (market != null && market.endsWith("USDT") && !market.startsWith("KRW-")) {
            return ExchangeMode.BINANCE;
        }
        return ExchangeMode.UPBIT;
    }
}
