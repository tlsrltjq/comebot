package com.giseop.comebot.mvp2.exchange.dto;

import com.giseop.comebot.mvp2.exchange.Exchange;

public record Mvp2ExchangeStatusResponse(
        Exchange exchange,
        String displayName,
        boolean enabled,
        boolean publicMarketDataOnly,
        boolean realTradingSupported,
        String marketData,
        String message
) {
}
