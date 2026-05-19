package com.giseop.comebot.config;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "strategy.simple-threshold")
public class StrategyProperties {

    private BigDecimal buyPrice = new BigDecimal("90000000");
    private BigDecimal sellPrice = new BigDecimal("110000000");
    private BigDecimal orderQuantity = new BigDecimal("0.01");
    private BigDecimal orderAmount = new BigDecimal("10000");
    private BigDecimal binanceOrderAmount = new BigDecimal("10");

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice == null ? new BigDecimal("90000000") : buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice == null ? new BigDecimal("110000000") : sellPrice;
    }

    public BigDecimal getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(BigDecimal orderQuantity) {
        this.orderQuantity = orderQuantity == null ? new BigDecimal("0.01") : orderQuantity;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount == null ? new BigDecimal("10000") : orderAmount;
    }

    public BigDecimal getOrderAmount(ExchangeMode exchange) {
        return exchange == ExchangeMode.BINANCE ? binanceOrderAmount : orderAmount;
    }

    public BigDecimal getBinanceOrderAmount() {
        return binanceOrderAmount;
    }

    public void setBinanceOrderAmount(BigDecimal binanceOrderAmount) {
        this.binanceOrderAmount = binanceOrderAmount == null ? new BigDecimal("10") : binanceOrderAmount;
    }
}
