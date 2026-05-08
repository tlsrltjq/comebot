package com.giseop.comebot.market.domain;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.time.Instant;

public record TickerSnapshot(
        ExchangeMode exchange,
        String market,
        BigDecimal tradePrice,
        BigDecimal accTradePrice24h,
        Instant capturedAt,
        PriceSource source
) {

    public TickerSnapshot {
        if (exchange == null) {
            throw new IllegalArgumentException("exchange must not be null");
        }
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("market must not be blank");
        }
        if (tradePrice == null || tradePrice.signum() <= 0) {
            throw new IllegalArgumentException("tradePrice must be positive");
        }
        capturedAt = capturedAt == null ? Instant.now() : capturedAt;
        source = source == null ? PriceSource.WEBSOCKET : source;
        market = market.trim().toUpperCase();
    }

    public MarketPrice toMarketPrice() {
        return new MarketPrice(market, tradePrice, capturedAt);
    }
}
