package com.giseop.comebot.strategy.service;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class StrategyMarketSettingsService {

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
}
