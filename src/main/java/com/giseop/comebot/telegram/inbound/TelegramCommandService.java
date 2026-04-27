package com.giseop.comebot.telegram.inbound;

import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
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

    private final TelegramCommandParser commandParser;
    private final TelegramCallbackParser callbackParser;
    private final TelegramNotificationSender telegramNotificationSender;
    private final com.giseop.comebot.telegram.sender.TelegramApiClient telegramApiClient;
    private final TelegramProperties telegramProperties;
    private final NotificationProperties notificationProperties;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final TradingFlowService tradingFlowService;
    private final TradingFlowHistoryService tradingFlowHistoryService;

    public TelegramCommandService(
            TelegramCommandParser commandParser,
            TelegramCallbackParser callbackParser,
            TelegramNotificationSender telegramNotificationSender,
            com.giseop.comebot.telegram.sender.TelegramApiClient telegramApiClient,
            TelegramProperties telegramProperties,
            NotificationProperties notificationProperties,
            TradingSchedulerProperties tradingSchedulerProperties,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService tradingFlowHistoryService
    ) {
        this.commandParser = commandParser;
        this.callbackParser = callbackParser;
        this.telegramNotificationSender = telegramNotificationSender;
        this.telegramApiClient = telegramApiClient;
        this.telegramProperties = telegramProperties;
        this.notificationProperties = notificationProperties;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.tradingFlowService = tradingFlowService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
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
                """.trim();
    }

    private String statusMessage() {
        return """
                Status
                telegram.enabled=%s
                telegram.configured=%s
                notification.enabled=%s
                scheduler.enabled=%s
                scheduler.markets=%s
                """.formatted(
                telegramProperties.isEnabled(),
                telegramProperties.isConfigured(),
                notificationProperties.isEnabled(),
                tradingSchedulerProperties.isEnabled(),
                tradingSchedulerProperties.getMarkets()
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
}
