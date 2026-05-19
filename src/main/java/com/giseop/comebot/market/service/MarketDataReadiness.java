package com.giseop.comebot.market.service;

import com.giseop.comebot.exchange.ExchangeMode;

public record MarketDataReadiness(
        ExchangeMode exchange,
        boolean ready,
        String reason,
        int snapshotCount,
        int freshSnapshotCount
) {

    public static MarketDataReadiness ready(ExchangeMode exchange, String reason) {
        return new MarketDataReadiness(exchange, true, reason, 0, 0);
    }

    public static MarketDataReadiness snapshot(
            ExchangeMode exchange,
            int snapshotCount,
            int freshSnapshotCount
    ) {
        if (freshSnapshotCount > 0) {
            return new MarketDataReadiness(
                    exchange,
                    true,
                    "Fresh ticker snapshot is available",
                    snapshotCount,
                    freshSnapshotCount
            );
        }
        return new MarketDataReadiness(
                exchange,
                false,
                "Fresh ticker snapshot is not available",
                snapshotCount,
                freshSnapshotCount
        );
    }
}
