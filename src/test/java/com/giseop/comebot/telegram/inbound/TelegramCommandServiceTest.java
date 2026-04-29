package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthResult;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.provider.MarketPriceProviderProperties;
import com.giseop.comebot.market.provider.MarketPriceProviderType;
import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
import com.giseop.comebot.telegram.sender.TelegramNotificationSender;
import com.giseop.comebot.trading.service.TradingFlowResult;
import com.giseop.comebot.trading.service.TradingFlowService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TelegramCommandServiceTest {

    @Test
    void runCommandCallsTradingFlowService() {
        TradingFlowService tradingFlowService = mock(TradingFlowService.class);
        when(tradingFlowService.run("KRW-BTC")).thenReturn(new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("100"),
                SignalType.BUY,
                "test",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.now()
        ));
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);

        service(sender, tradingFlowService).handle("/run KRW-BTC");

        verify(tradingFlowService).run("KRW-BTC");
        verify(sender).sendMessage(any(NotificationMessage.class));
    }

    @Test
    void unknownCommandSendsHelpMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handle("/unknown");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("/help", "/status", "/run KRW-BTC", "/history KRW-BTC");
    }

    @Test
    void menuCommandSendsInlineKeyboard() {
        TelegramApiClient apiClient = mock(TelegramApiClient.class);

        service(mock(TelegramNotificationSender.class), apiClient, mock(TradingFlowService.class), mock(TradingFlowHistoryService.class))
                .handle("/menu");

        verify(apiClient).sendMessage(
                org.mockito.Mockito.eq("token"),
                org.mockito.Mockito.eq("chat-id"),
                org.mockito.Mockito.contains("메뉴"),
                any(TelegramInlineKeyboard.class)
        );
    }

    @Test
    void statusCallbackSendsStatusMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handleCallback("STATUS");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "DB connected: true",
                "Market Provider: IN_MEMORY",
                "Strategy: SimpleThresholdStrategy",
                "Buy Price: 90000000",
                "Sell Price: 110000000",
                "Order Quantity: 0.001",
                "Max Order Amount: 100000",
                "Allowed Markets: [KRW-BTC, KRW-ETH]",
                "Scheduler Enabled: false",
                "Notification Enabled: false",
                "Telegram Enabled: true",
                "Telegram Inbound Enabled: false"
        );
    }

    @Test
    void statusCommandDoesNotExposeSensitiveValues() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handle("/status");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body())
                .doesNotContain("token", "chat-id", "password", "secret");
    }

    @Test
    void runCallbackCallsTradingFlowService() {
        TradingFlowService tradingFlowService = mock(TradingFlowService.class);
        when(tradingFlowService.run("KRW-BTC")).thenReturn(new TradingFlowResult(
                "KRW-BTC",
                new BigDecimal("100"),
                SignalType.BUY,
                "test",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.now()
        ));

        service(mock(TelegramNotificationSender.class), tradingFlowService).handleCallback("RUN:KRW-BTC");

        verify(tradingFlowService).run("KRW-BTC");
    }

    @Test
    void historyCallbackSendsMarketHistory() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        TradingFlowHistoryService historyService = mock(TradingFlowHistoryService.class);
        when(historyService.findRecent("KRW-BTC", 5)).thenReturn(List.of(new TradingFlowHistory(
                "history-1",
                "KRW-BTC",
                new BigDecimal("100"),
                SignalType.BUY,
                "test",
                true,
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.parse("2026-04-27T00:00:00Z")
        )));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TelegramApiClient.class), mock(TradingFlowService.class), historyService)
                .handleCallback("HISTORY:KRW-BTC");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("Recent history for market=KRW-BTC", "FILLED");
    }

    @Test
    void helpCallbackSendsHelpMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handleCallback("HELP");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("/menu");
    }

    @Test
    void portfolioCommandSendsPortfolioSummary() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService()).handle("/portfolio");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "Paper portfolio",
                "cash=1000000",
                "totalEquity=1100000",
                "realizedProfit=50000",
                "unrealizedProfit=50000",
                "totalProfit=100000"
        );
    }

    @Test
    void positionsCommandSendsPositionList() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(
                sender,
                mock(TradingFlowService.class),
                portfolioService(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("90000000")))),
                valuationService()
        ).handle("/positions");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "Paper positions",
                "market=KRW-BTC",
                "quantity=0.01",
                "averageBuyPrice=90000000"
        );
    }

    @Test
    void positionsCommandSendsEmptyPositionMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService()).handle("/positions");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("No paper positions");
    }

    @Test
    void portfolioCommandSendsFailureMessageWhenValuationFails() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        PaperPortfolioValuationService valuationService = mock(PaperPortfolioValuationService.class);
        when(valuationService.valuate()).thenThrow(new IllegalStateException("Current price is not available"));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService).handle("/portfolio");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("Portfolio valuation failed");
    }

    @Test
    void portfolioCallbackSendsPortfolioSummary() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService()).handleCallback("PORTFOLIO");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("Paper portfolio", "totalEquity=1100000");
    }

    @Test
    void positionsCallbackSendsPositionList() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(
                sender,
                mock(TradingFlowService.class),
                portfolioService(List.of(new PaperPosition("KRW-ETH", new BigDecimal("0.2"), new BigDecimal("3000000")))),
                valuationService()
        ).handleCallback("POSITIONS");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("Paper positions", "market=KRW-ETH");
    }

    @Test
    void unknownCallbackSendsHelpMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handleCallback("UNKNOWN");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("/help");
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TradingFlowService tradingFlowService
    ) {
        return service(sender, mock(TelegramApiClient.class), tradingFlowService, mock(TradingFlowHistoryService.class));
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TradingFlowService tradingFlowService,
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService
    ) {
        return service(
                sender,
                mock(TelegramApiClient.class),
                tradingFlowService,
                mock(TradingFlowHistoryService.class),
                paperPortfolioService,
                paperPortfolioValuationService
        );
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TelegramApiClient telegramApiClient,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService historyService
    ) {
        return service(sender, telegramApiClient, tradingFlowService, historyService, portfolioService(List.of()), valuationService());
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TelegramApiClient telegramApiClient,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService historyService,
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService
    ) {
        return new TelegramCommandService(
                new TelegramCommandParser(),
                new TelegramCallbackParser(),
                sender,
                telegramApiClient,
                databaseHealthService(),
                marketPriceProviderProperties(),
                strategyProperties(),
                tradingProperties(),
                configuredTelegramProperties(),
                new TelegramInboundProperties(),
                new NotificationProperties(),
                new TradingSchedulerProperties(),
                tradingFlowService,
                historyService,
                paperPortfolioService,
                paperPortfolioValuationService
        );
    }

    private PaperPortfolioService portfolioService(List<PaperPosition> positions) {
        PaperPortfolioService service = mock(PaperPortfolioService.class);
        when(service.findPositions()).thenReturn(positions);
        return service;
    }

    private PaperPortfolioValuationService valuationService() {
        PaperPortfolioValuationService service = mock(PaperPortfolioValuationService.class);
        when(service.valuate()).thenReturn(new PortfolioValuationResponse(
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                new BigDecimal("1100000"),
                new BigDecimal("50000"),
                new BigDecimal("50000"),
                new BigDecimal("100000"),
                List.of()
        ));
        return service;
    }

    private TelegramProperties configuredTelegramProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }

    private DatabaseHealthService databaseHealthService() {
        DatabaseHealthService service = mock(DatabaseHealthService.class);
        when(service.check()).thenReturn(new DatabaseHealthResult(true, "PostgreSQL"));
        return service;
    }

    private MarketPriceProviderProperties marketPriceProviderProperties() {
        MarketPriceProviderProperties properties = mock(MarketPriceProviderProperties.class);
        when(properties.getPriceProvider()).thenReturn(MarketPriceProviderType.IN_MEMORY);
        return properties;
    }

    private StrategyProperties strategyProperties() {
        StrategyProperties properties = mock(StrategyProperties.class);
        when(properties.getBuyPrice()).thenReturn(new BigDecimal("90000000"));
        when(properties.getSellPrice()).thenReturn(new BigDecimal("110000000"));
        when(properties.getOrderQuantity()).thenReturn(new BigDecimal("0.001"));
        return properties;
    }

    private TradingProperties tradingProperties() {
        TradingProperties properties = mock(TradingProperties.class);
        when(properties.getMaxOrderAmount()).thenReturn(new BigDecimal("100000"));
        when(properties.getAllowedMarkets()).thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        return properties;
    }
}
