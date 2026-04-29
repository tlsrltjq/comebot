package com.giseop.comebot.telegram.inbound;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import com.giseop.comebot.trading.service.TradingFlowResult;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TelegramCommandService {

    private static final int HISTORY_LIMIT = 5;
    private static final String STRATEGY_NAME = "SimpleThresholdStrategy";

    private final TelegramCommandParser commandParser;
    private final TelegramCallbackParser callbackParser;
    private final TelegramNotificationSender telegramNotificationSender;
    private final com.giseop.comebot.telegram.sender.TelegramApiClient telegramApiClient;
    private final DatabaseHealthService databaseHealthService;
    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final StrategyProperties strategyProperties;
    private final TradingProperties tradingProperties;
    private final TelegramProperties telegramProperties;
    private final TelegramInboundProperties telegramInboundProperties;
    private final NotificationProperties notificationProperties;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final SafetyProperties safetyProperties;
    private final TradingFlowService tradingFlowService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final PaperPortfolioService paperPortfolioService;
    private final PaperPortfolioValuationService paperPortfolioValuationService;

    public TelegramCommandService(
            TelegramCommandParser commandParser,
            TelegramCallbackParser callbackParser,
            TelegramNotificationSender telegramNotificationSender,
            com.giseop.comebot.telegram.sender.TelegramApiClient telegramApiClient,
            DatabaseHealthService databaseHealthService,
            MarketPriceProviderProperties marketPriceProviderProperties,
            StrategyProperties strategyProperties,
            TradingProperties tradingProperties,
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties,
            NotificationProperties notificationProperties,
            TradingSchedulerProperties tradingSchedulerProperties,
            SafetyProperties safetyProperties,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService
    ) {
        this.commandParser = commandParser;
        this.callbackParser = callbackParser;
        this.telegramNotificationSender = telegramNotificationSender;
        this.telegramApiClient = telegramApiClient;
        this.databaseHealthService = databaseHealthService;
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.strategyProperties = strategyProperties;
        this.tradingProperties = tradingProperties;
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
        this.notificationProperties = notificationProperties;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.safetyProperties = safetyProperties;
        this.tradingFlowService = tradingFlowService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.paperPortfolioService = paperPortfolioService;
        this.paperPortfolioValuationService = paperPortfolioValuationService;
    }

    public void handle(String text) {
        TelegramCommand command = commandParser.parse(text);
        String response = switch (command.type()) {
            case HELP, UNKNOWN -> helpMessage();
            case MENU -> {
                sendMenu();
                yield null;
            }
            case STATUS -> statusMessage();
            case RUN -> runMessage(command.market());
            case HISTORY -> historyMessage(command.market());
            case PORTFOLIO -> portfolioMessage();
            case POSITIONS -> positionsMessage();
        };
        if (response != null) {
            sendText(response);
        }
    }

    public void handleCallback(String data) {
        TelegramCallback callback = callbackParser.parse(data);
        String response = switch (callback.type()) {
            case HELP, UNKNOWN -> helpMessage();
            case STATUS -> statusMessage();
            case RUN -> runMessage(callback.market());
            case HISTORY -> historyMessage(callback.market());
            case PORTFOLIO -> portfolioMessage();
            case POSITIONS -> positionsMessage();
        };
        sendText(response);
    }

    private void sendMenu() {
        if (!telegramProperties.isEnabled() || !telegramProperties.isConfigured()) {
            return;
        }
        telegramApiClient.sendMessage(
                telegramProperties.getBotToken(),
                telegramProperties.getChatId(),
                "메뉴에서 실행할 기능을 선택하세요.",
                TelegramInlineKeyboard.mainMenu()
        );
    }

    private void sendText(String response) {
        telegramNotificationSender.sendMessage(new NotificationMessage("Telegram command response", response, Instant.now()));
    }

    private String helpMessage() {
        return """
                Available commands:
                /help
                /menu
                /status
                /run KRW-BTC
                /history KRW-BTC
                /portfolio
                /positions
                """.trim();
    }

    private String statusMessage() {
        return """
                System status
                DB connected: %s
                Market Provider: %s
                Strategy: %s
                Buy Price: %s
                Sell Price: %s
                Order Quantity: %s
                Max Order Amount: %s
                Allowed Markets: %s
                Scheduler Enabled: %s
                Kill Switch Enabled: %s
                Notification Enabled: %s
                Telegram Enabled: %s
                Telegram Inbound Enabled: %s
                """.formatted(
                databaseHealthService.check().connected(),
                marketPriceProviderProperties.getPriceProvider(),
                STRATEGY_NAME,
                strategyProperties.getBuyPrice(),
                strategyProperties.getSellPrice(),
                strategyProperties.getOrderQuantity(),
                tradingProperties.getMaxOrderAmount(),
                tradingProperties.getAllowedMarkets(),
                tradingSchedulerProperties.isEnabled(),
                safetyProperties.isKillSwitchEnabled(),
                notificationProperties.isEnabled(),
                telegramProperties.isEnabled(),
                telegramInboundProperties.isEnabled()
        ).trim();
    }

    private String runMessage(String market) {
        if (market == null || market.isBlank()) {
            return "Usage: /run KRW-BTC";
        }

        TradingFlowResult result = tradingFlowService.run(market);
        return """
                Trading flow result
                market=%s
                signal=%s
                orderCreated=%s
                orderStatus=%s
                message=%s
                """.formatted(
                result.market(),
                result.signalType(),
                result.orderCreated(),
                result.orderStatus(),
                result.message()
        ).trim();
    }

    private String historyMessage(String market) {
        if (market == null || market.isBlank()) {
            return "Usage: /history KRW-BTC";
        }

        List<TradingFlowHistory> histories = tradingFlowHistoryService.findRecent(market, HISTORY_LIMIT);
        if (histories.isEmpty()) {
            return "No history for market=%s".formatted(market);
        }

        StringBuilder builder = new StringBuilder("Recent history for market=").append(market);
        for (TradingFlowHistory history : histories) {
            builder.append(System.lineSeparator())
                    .append("- ")
                    .append(history.createdAt())
                    .append(" ")
                    .append(history.signalType())
                    .append(" ")
                    .append(history.orderStatus())
                    .append(" ")
                    .append(history.message());
        }
        return builder.toString();
    }

    private String portfolioMessage() {
        try {
            PortfolioValuationResponse valuation = paperPortfolioValuationService.valuate();
            return """
                    Paper portfolio
                    cash=%s
                    totalEquity=%s
                    realizedProfit=%s
                    unrealizedProfit=%s
                    totalProfit=%s
                    """.formatted(
                    valuation.cash(),
                    valuation.totalEquity(),
                    valuation.realizedProfit(),
                    valuation.unrealizedProfit(),
                    valuation.totalProfit()
            ).trim();
        } catch (RuntimeException e) {
            return "Portfolio valuation failed: current price is not available";
        }
    }

    private String positionsMessage() {
        List<PaperPosition> positions = paperPortfolioService.findPositions();
        if (positions.isEmpty()) {
            return "No paper positions";
        }

        StringBuilder builder = new StringBuilder("Paper positions");
        for (PaperPosition position : positions) {
            builder.append(System.lineSeparator())
                    .append("- market=")
                    .append(position.market())
                    .append(", quantity=")
                    .append(position.quantity())
                    .append(", averageBuyPrice=")
                    .append(position.averageBuyPrice());
        }
        return builder.toString();
    }
}
