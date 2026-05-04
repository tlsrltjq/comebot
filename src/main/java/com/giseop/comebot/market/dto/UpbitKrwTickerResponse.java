package com.giseop.comebot.market.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpbitKrwTickerResponse(
        String market,
        BigDecimal tradePrice,
        BigDecimal signedChangeRate,
        BigDecimal accTradePrice24h
) {

    @JsonCreator
    public UpbitKrwTickerResponse(
            @JsonProperty("market") String market,
            @JsonProperty("trade_price") BigDecimal tradePrice,
            @JsonProperty("signed_change_rate") BigDecimal signedChangeRate,
            @JsonProperty("acc_trade_price_24h") BigDecimal accTradePrice24h
    ) {
        this.market = market;
        this.tradePrice = tradePrice;
        this.signedChangeRate = signedChangeRate;
        this.accTradePrice24h = accTradePrice24h;
    }
}
