package com.giseop.comebot.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    private BigDecimal maxOrderAmount = new BigDecimal("100000");
    private List<String> allowedMarkets = new ArrayList<>(List.of("KRW-BTC", "KRW-ETH"));

    public BigDecimal getMaxOrderAmount() {
        return maxOrderAmount;
    }

    public void setMaxOrderAmount(BigDecimal maxOrderAmount) {
        this.maxOrderAmount = maxOrderAmount == null ? new BigDecimal("100000") : maxOrderAmount;
    }

    public List<String> getAllowedMarkets() {
        return allowedMarkets;
    }

    public void setAllowedMarkets(List<String> allowedMarkets) {
        this.allowedMarkets = allowedMarkets == null ? new ArrayList<>() : allowedMarkets;
    }
}
