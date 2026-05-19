package com.giseop.comebot.portfolio;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "paper.fee")
public class PaperFeeProperties {

    private BigDecimal upbitRate = new BigDecimal("0.0005");
    private BigDecimal binanceRate = new BigDecimal("0.001");

    public BigDecimal getUpbitRate() {
        return upbitRate;
    }

    public void setUpbitRate(BigDecimal upbitRate) {
        this.upbitRate = upbitRate == null ? new BigDecimal("0.0005") : upbitRate;
    }

    public BigDecimal getBinanceRate() {
        return binanceRate;
    }

    public void setBinanceRate(BigDecimal binanceRate) {
        this.binanceRate = binanceRate == null ? new BigDecimal("0.001") : binanceRate;
    }

    public BigDecimal rate(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? binanceRate : upbitRate;
    }
}
