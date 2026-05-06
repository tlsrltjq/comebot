package com.giseop.comebot.mvp2.exchange.dto;

import com.giseop.comebot.mvp2.exchange.Exchange;

public record Mvp2ExchangeResponse(
        Exchange exchange,
        String displayName,
        boolean enabled,
        boolean publicMarketDataOnly,
        String statusPath
) {
}
