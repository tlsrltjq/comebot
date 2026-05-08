package com.giseop.comebot.system.controller;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.system.dto.SystemStatusResponse;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.inbound.TelegramInboundProperties;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    private final DatabaseHealthService databaseHealthService;
    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final StrategyProperties strategyProperties;
    private final StrategySelectionProperties strategySelectionProperties;
    private final TradingProperties tradingProperties;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final PaperPortfolioService paperPortfolioService;
    private final SafetyProperties safetyProperties;
    private final NotificationProperties notificationProperties;
    private final TelegramProperties telegramProperties;
    private final TelegramInboundProperties telegramInboundProperties;

    public SystemStatusController(
            DatabaseHealthService databaseHealthService,
            MarketPriceProviderProperties marketPriceProviderProperties,
            StrategyProperties strategyProperties,
            StrategySelectionProperties strategySelectionProperties,
            TradingProperties tradingProperties,
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PaperPortfolioService paperPortfolioService,
            SafetyProperties safetyProperties,
            NotificationProperties notificationProperties,
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties
    ) {
        this.databaseHealthService = databaseHealthService;
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.strategyProperties = strategyProperties;
        this.strategySelectionProperties = strategySelectionProperties;
        this.tradingProperties = tradingProperties;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.paperPortfolioService = paperPortfolioService;
        this.safetyProperties = safetyProperties;
        this.notificationProperties = notificationProperties;
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
    }

    @GetMapping("/api/system/status")
    public ResponseEntity<SystemStatusResponse> getStatus(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);

        MarketPriceProviderType provider = marketPriceProviderProperties.getPriceProvider();
        return ResponseEntity.ok(new SystemStatusResponse(
                new SystemStatusResponse.DatabaseStatus(databaseHealthService.check().connected()),
                new SystemStatusResponse.MarketProviderStatus(
                        provider,
                        provider == MarketPriceProviderType.UPBIT
                                || provider == MarketPriceProviderType.BINANCE
                                || provider == MarketPriceProviderType.SNAPSHOT
                ),
                new SystemStatusResponse.StrategyStatus(
                        strategySelectionProperties.getStrategyName(),
                        strategyProperties.getBuyPrice(),
                        strategyProperties.getSellPrice(),
                        strategyProperties.getOrderQuantity(),
                        strategyProperties.getOrderAmount()
                ),
                new SystemStatusResponse.RiskStatus(
                        tradingProperties.getMaxOrderAmount(),
                        new ArrayList<>(tradingProperties.getAllowedMarkets())
                ),
                new SystemStatusResponse.SchedulerStatus(
                        tradingSchedulerProperties.isEnabled(),
                        tradingSchedulerProperties.getFixedDelayMs(),
                        new ArrayList<>(tradingSchedulerProperties.getMarkets()),
                        candidateSchedulerProperties.isEnabled(),
                        candidateSchedulerProperties.getFixedDelayMs(),
                        new ArrayList<>(candidateSchedulerProperties.getMarkets()),
                        candidateSchedulerProperties.isNotifySummary(),
                        positionExitSchedulerProperties.isEnabled(),
                        positionExitSchedulerProperties.getFixedDelayMs(),
                        positionExitSchedulerProperties.isSaveHoldHistory(),
                        positionExitSchedulerProperties.getExchange().name(),
                        exitPositionMarketCount()
                ),
                new SystemStatusResponse.SafetyStatus(
                        safetyProperties.isKillSwitchEnabled()
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
                        telegramInboundProperties.isEnabled(),
                        telegramInboundProperties.isManualPaperExecutionEnabled()
                )
        ));
    }

    private int exitPositionMarketCount() {
        return (int) paperPortfolioService.findPositions(positionExitSchedulerProperties.getExchange()).stream()
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .map(position -> position.market())
                .filter(market -> market != null && !market.isBlank())
                .distinct()
                .count();
    }
}
