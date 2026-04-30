package com.giseop.comebot.strategy.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.market-overrides")
public class StrategyMarketOverrideProperties {

    private Map<String, MarketOverride> markets = new HashMap<>();

    public Map<String, MarketOverride> getMarkets() {
        return markets;
    }

    public void setMarkets(Map<String, MarketOverride> markets) {
        this.markets = markets == null ? new HashMap<>() : markets;
    }

    public MarketOverride get(String market) {
        return market == null ? null : markets.get(market);
    }

    public static class MarketOverride {

        private BigDecimal orderQuantity;
        private BigDecimal minPriceChangeRate;
        private BigDecimal minTradeAmountChangeRate;
        private BigDecimal maxPriceChangeRate;
        private BigDecimal maxHighLowRangeRate;

        public BigDecimal getOrderQuantity() {
            return orderQuantity;
        }

        public void setOrderQuantity(BigDecimal orderQuantity) {
            this.orderQuantity = orderQuantity;
        }

        public BigDecimal getMinPriceChangeRate() {
            return minPriceChangeRate;
        }

        public void setMinPriceChangeRate(BigDecimal minPriceChangeRate) {
            this.minPriceChangeRate = minPriceChangeRate;
        }

        public BigDecimal getMinTradeAmountChangeRate() {
            return minTradeAmountChangeRate;
        }

        public void setMinTradeAmountChangeRate(BigDecimal minTradeAmountChangeRate) {
            this.minTradeAmountChangeRate = minTradeAmountChangeRate;
        }

        public BigDecimal getMaxPriceChangeRate() {
            return maxPriceChangeRate;
        }

        public void setMaxPriceChangeRate(BigDecimal maxPriceChangeRate) {
            this.maxPriceChangeRate = maxPriceChangeRate;
        }

        public BigDecimal getMaxHighLowRangeRate() {
            return maxHighLowRangeRate;
        }

        public void setMaxHighLowRangeRate(BigDecimal maxHighLowRangeRate) {
            this.maxHighLowRangeRate = maxHighLowRangeRate;
        }
    }
}
