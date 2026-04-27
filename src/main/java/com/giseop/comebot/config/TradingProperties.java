package com.giseop.comebot.config;

import com.giseop.comebot.execution.domain.ExecutionMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {

    private ExecutionMode mode = ExecutionMode.PAPER_TRADING;
    private BigDecimal maxOrderAmount = new BigDecimal("100000");
    private List<String> allowedMarkets = new ArrayList<>(List.of("KRW-BTC", "KRW-ETH"));

    public ExecutionMode getMode() {
        return mode;
    }

    public void setMode(ExecutionMode mode) {
        this.mode = mode == null ? ExecutionMode.PAPER_TRADING : mode;
    }

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
