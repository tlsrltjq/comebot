package com.giseop.comebot.system.dto;

import com.giseop.comebot.market.provider.MarketPriceProviderType;
import java.math.BigDecimal;
import java.util.List;

public record SystemStatusResponse(
        DatabaseStatus database,
        MarketProviderStatus marketProvider,
        StrategyStatus strategy,
        RiskStatus risk,
        SchedulerStatus scheduler,
        SafetyStatus safety,
        NotificationStatus notification,
        TelegramStatus telegram
) {

    public record DatabaseStatus(
            boolean connected
    ) {
    }

    public record MarketProviderStatus(
            MarketPriceProviderType provider,
            boolean externalProvider
    ) {
    }

    public record StrategyStatus(
            String strategyName,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            BigDecimal orderQuantity,
            BigDecimal orderAmount
    ) {
    }

    public record RiskStatus(
            BigDecimal maxOrderAmount,
            List<String> allowedMarkets
    ) {
    }

    public record SchedulerStatus(
            boolean enabled,
            long fixedDelayMs,
            List<String> markets,
            boolean candidateEnabled,
            long candidateFixedDelayMs,
            List<String> candidateMarkets,
            boolean candidateNotifySummary,
            String candidateExchange,
            boolean exitEnabled,
            long exitFixedDelayMs,
            boolean exitSaveHoldHistory,
            String exitExchange,
            int exitPositionMarketCount
    ) {
    }

    public record SafetyStatus(
            boolean killSwitchEnabled
    ) {
    }

    public record NotificationStatus(
            boolean enabled,
            boolean sendHold,
            boolean sendFilled,
            boolean sendRejected
    ) {
    }

    public record TelegramStatus(
            boolean enabled,
            boolean configured,
            boolean inboundEnabled,
            boolean manualPaperExecutionEnabled
    ) {
    }
}
