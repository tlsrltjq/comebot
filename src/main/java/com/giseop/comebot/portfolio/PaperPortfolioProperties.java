package com.giseop.comebot.portfolio;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "paper")
public class PaperPortfolioProperties {

    private BigDecimal initialCash = new BigDecimal("1000000");
    private String portfolioStorageType = "IN_MEMORY";

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash == null ? new BigDecimal("1000000") : initialCash;
    }

    public String getPortfolioStorageType() {
        return portfolioStorageType;
    }

    public void setPortfolioStorageType(String portfolioStorageType) {
        this.portfolioStorageType = portfolioStorageType == null ? "IN_MEMORY" : portfolioStorageType;
    }
}
