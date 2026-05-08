package com.giseop.comebot.market.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceUsdtTickerResponse(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal quoteVolume
) {

    @JsonCreator
    public BinanceUsdtTickerResponse(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("lastPrice") BigDecimal lastPrice,
            @JsonProperty("quoteVolume") BigDecimal quoteVolume
    ) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.quoteVolume = quoteVolume;
    }
}
