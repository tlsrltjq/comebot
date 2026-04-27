package com.giseop.comebot.system.controller;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.system.dto.SystemStatusResponse;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.inbound.TelegramInboundProperties;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    private static final String STRATEGY_NAME = "SimpleThresholdStrategy";

    private final DatabaseHealthService databaseHealthService;
    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final StrategyProperties strategyProperties;
    private final TradingProperties tradingProperties;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final NotificationProperties notificationProperties;
    private final TelegramProperties telegramProperties;
    private final TelegramInboundProperties telegramInboundProperties;

    public SystemStatusController(
            DatabaseHealthService databaseHealthService,
            MarketPriceProviderProperties marketPriceProviderProperties,
            StrategyProperties strategyProperties,
            TradingProperties tradingProperties,
            TradingSchedulerProperties tradingSchedulerProperties,
            NotificationProperties notificationProperties,
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties
    ) {
        this.databaseHealthService = databaseHealthService;
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.strategyProperties = strategyProperties;
        this.tradingProperties = tradingProperties;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.notificationProperties = notificationProperties;
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
    }

    @GetMapping("/api/system/status")
    public SystemStatusResponse getStatus() {
        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        return new SystemStatusResponse(
                new SystemStatusResponse.DatabaseStatus(databaseHealthService.check().connected()),
                new SystemStatusResponse.MarketProviderStatus(provider, provider == MarketPriceProviderType.UPBIT),
                new SystemStatusResponse.StrategyStatus(
                        STRATEGY_NAME,
                        strategyProperties.getBuyPrice(),
                        strategyProperties.getSellPrice(),
                        strategyProperties.getOrderQuantity()
                ),
                new SystemStatusResponse.RiskStatus(
                        tradingProperties.getMaxOrderAmount(),
                        new ArrayList<>(tradingProperties.getAllowedMarkets())
                ),
                new SystemStatusResponse.SchedulerStatus(
                        tradingSchedulerProperties.isEnabled(),
                        tradingSchedulerProperties.getFixedDelayMs(),
                        new ArrayList<>(tradingSchedulerProperties.getMarkets())
                ),
                new SystemStatusResponse.NotificationStatus(
                        notificationProperties.isEnabled(),
                        notificationProperties.isSendHold(),
                        notificationProperties.isSendFilled(),
                        notificationProperties.isSendRejected()
                ),
                new SystemStatusResponse.TelegramStatus(
                        telegramProperties.isEnabled(),
                        telegramProperties.isConfigured(),
                        telegramInboundProperties.isEnabled()
                )
        );
    }
}
