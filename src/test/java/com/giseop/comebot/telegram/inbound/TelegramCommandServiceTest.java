package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationMessage;
import com.giseop.comebot.notification.NotificationProperties;
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
        assertThat(messageCaptor.getValue().body()).contains("telegram.enabled=true", "scheduler.enabled=false");
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
            TelegramApiClient telegramApiClient,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService historyService
    ) {
        return new TelegramCommandService(
                new TelegramCommandParser(),
                new TelegramCallbackParser(),
                sender,
                telegramApiClient,
                configuredTelegramProperties(),
                new NotificationProperties(),
                new TradingSchedulerProperties(),
                tradingFlowService,
                historyService
        );
    }

    private TelegramProperties configuredTelegramProperties() {
        TelegramProperties properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("token");
        properties.setChatId("chat-id");
        return properties;
    }
}
