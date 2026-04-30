package com.giseop.comebot.telegram.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.database.DatabaseHealthResult;
import com.giseop.comebot.database.DatabaseHealthService;
import com.giseop.comebot.execution.domain.OrderStatus;
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
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.indicator.MarketTrend;
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
        when(tradingFlowService.run("KRW-BTC")).thenReturn(flowResult("KRW-BTC", SignalType.BUY, true, OrderStatus.FILLED));

        service(mock(TelegramNotificationSender.class), tradingFlowService).handle("/run KRW-BTC");

        verify(tradingFlowService).run("KRW-BTC");
    }

    @Test
    void unknownCommandSendsKoreanHelpMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handle("/unknown");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "사용 가능한 명령",
                "/candidates",
                "/candidate-run KRW-BTC",
                "/run KRW-BTC"
        );
    }

    @Test
    void riskCommandSendsRiskPolicyMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handle("/risk");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "리스크 정책",
                "maxOrderAmount=100000",
                "allowedMarkets=[KRW-BTC, KRW-ETH]",
                "takeProfitRate=5",
                "stopLossRate=-3",
                "positionExitEnabled=false",
                "dailyRiskEnabled=false",
                "dailyOrderLimit=10",
                "dailyLossLimit=50000"
        );
    }

    @Test
    void safetyCommandSendsSafetyMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handle("/safety");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("안전장치 상태", "killSwitchEnabled=false");
    }

    @Test
    void menuCommandSendsKoreanInlineKeyboard() {
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
    void statusCallbackSendsKoreanStatusMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handleCallback("STATUS");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "시스템 상태",
                "DB 연결: true",
                "시세 Provider: IN_MEMORY",
                "전략: SimpleThresholdStrategy",
                "매수 기준가: 90000000",
                "매도 기준가: 110000000",
                "주문 수량: 0.001",
                "최대 주문 금액: 100000",
                "허용 Market: [KRW-BTC, KRW-ETH]",
                "스케줄러 활성화: false",
                "후보 스케줄러 활성화: false",
                "긴급 정지: false",
                "알림 활성화: false",
                "텔레그램 활성화: true",
                "텔레그램 수신 활성화: false"
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
        when(tradingFlowService.run("KRW-BTC")).thenReturn(flowResult("KRW-BTC", null, false, OrderStatus.REJECTED));

        service(mock(TelegramNotificationSender.class), tradingFlowService).handleCallback("RUN:KRW-BTC");

        verify(tradingFlowService).run("KRW-BTC");
    }

    @Test
    void runCommandSendsBlockedMessageWhenKillSwitchBlocksTradingFlow() {
        TradingFlowService tradingFlowService = mock(TradingFlowService.class);
        when(tradingFlowService.run("KRW-BTC")).thenReturn(new TradingFlowResult(
                "KRW-BTC",
                null,
                null,
                "Kill switch enabled",
                false,
                OrderStatus.REJECTED,
                "Kill switch enabled: trading flow blocked",
                Instant.now()
        ));
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, tradingFlowService).handle("/run KRW-BTC");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("Kill switch enabled: trading flow blocked");
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
        assertThat(messageCaptor.getValue().body()).contains("최근 실행 이력 market=KRW-BTC", "FILLED");
    }

    @Test
    void candidatesCommandSendsCandidateList() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        CandidateScannerService scannerService = mock(CandidateScannerService.class);
        when(scannerService.scanAllowedMarkets()).thenReturn(List.of(candidate("KRW-BTC", CandidateDecision.SELECTED)));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), scannerService, mock(CandidateExecutionService.class)).handle("/candidates");

        verify(scannerService).scanAllowedMarkets();
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("롱 후보 목록", "market=KRW-BTC", "decision=SELECTED");
    }

    @Test
    void candidatesCallbackSendsCandidateList() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        CandidateScannerService scannerService = mock(CandidateScannerService.class);
        when(scannerService.scanAllowedMarkets()).thenReturn(List.of(candidate("KRW-ETH", CandidateDecision.SKIPPED)));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), scannerService, mock(CandidateExecutionService.class)).handleCallback("CANDIDATES");

        verify(scannerService).scanAllowedMarkets();
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("롱 후보 목록", "market=KRW-ETH", "decision=SKIPPED");
    }

    @Test
    void candidateRunCommandCallsCandidateExecutionService() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        CandidateExecutionService executionService = mock(CandidateExecutionService.class);
        when(executionService.execute("KRW-BTC")).thenReturn(flowResult("KRW-BTC", SignalType.BUY, true, OrderStatus.FILLED));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), mock(CandidateScannerService.class), executionService)
                .handle("/candidate-run KRW-BTC");

        verify(executionService).execute("KRW-BTC");
        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("후보 PAPER 실행 결과", "market=KRW-BTC", "orderStatus=FILLED");
    }

    @Test
    void candidateRunCallbackCallsCandidateExecutionService() {
        CandidateExecutionService executionService = mock(CandidateExecutionService.class);
        when(executionService.execute("KRW-BTC")).thenReturn(flowResult("KRW-BTC", SignalType.BUY, true, OrderStatus.FILLED));

        service(mock(TelegramNotificationSender.class), mock(TradingFlowService.class), mock(CandidateScannerService.class), executionService)
                .handleCallback("CANDIDATE_RUN:KRW-BTC");

        verify(executionService).execute("KRW-BTC");
    }

    @Test
    void portfolioCommandSendsPortfolioSummary() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService()).handle("/portfolio");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains(
                "PAPER 포트폴리오",
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
                "보유 포지션",
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
        assertThat(messageCaptor.getValue().body()).contains("보유 포지션이 없습니다.");
    }

    @Test
    void portfolioCommandSendsFailureMessageWhenValuationFails() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        PaperPortfolioValuationService valuationService = mock(PaperPortfolioValuationService.class);
        when(valuationService.valuate()).thenThrow(new IllegalStateException("Current price is not available"));
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class), portfolioService(List.of()), valuationService).handle("/portfolio");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("포트폴리오 평가 실패");
    }

    @Test
    void unknownCallbackSendsHelpMessage() {
        TelegramNotificationSender sender = mock(TelegramNotificationSender.class);
        ArgumentCaptor<NotificationMessage> messageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

        service(sender, mock(TradingFlowService.class)).handleCallback("UNKNOWN");

        verify(sender).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).contains("/help");
    }

    private TelegramCommandService service(TelegramNotificationSender sender, TradingFlowService tradingFlowService) {
        return service(sender, mock(TelegramApiClient.class), tradingFlowService, mock(TradingFlowHistoryService.class));
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TradingFlowService tradingFlowService,
            CandidateScannerService candidateScannerService,
            CandidateExecutionService candidateExecutionService
    ) {
        return service(
                sender,
                mock(TelegramApiClient.class),
                tradingFlowService,
                mock(TradingFlowHistoryService.class),
                portfolioService(List.of()),
                valuationService(),
                candidateScannerService,
                candidateExecutionService
        );
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
                paperPortfolioValuationService,
                defaultCandidateScannerService(),
                mock(CandidateExecutionService.class)
        );
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TelegramApiClient telegramApiClient,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService historyService
    ) {
        return service(
                sender,
                telegramApiClient,
                tradingFlowService,
                historyService,
                portfolioService(List.of()),
                valuationService(),
                defaultCandidateScannerService(),
                mock(CandidateExecutionService.class)
        );
    }

    private TelegramCommandService service(
            TelegramNotificationSender sender,
            TelegramApiClient telegramApiClient,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService historyService,
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService,
            CandidateScannerService candidateScannerService,
            CandidateExecutionService candidateExecutionService
    ) {
        return new TelegramCommandService(
                new TelegramCommandParser(),
                new TelegramCallbackParser(),
                sender,
                telegramApiClient,
                databaseHealthService(),
                marketPriceProviderProperties(),
                strategyProperties(),
                strategySelectionProperties(),
                tradingProperties(),
                configuredTelegramProperties(),
                new TelegramInboundProperties(),
                new NotificationProperties(),
                new TradingSchedulerProperties(),
                new CandidateSchedulerProperties(),
                new SafetyProperties(),
                positionExitProperties(),
                dailyRiskProperties(),
                tradingFlowService,
                historyService,
                paperPortfolioService,
                paperPortfolioValuationService,
                candidateScannerService,
                candidateExecutionService
        );
    }

    private CandidateScannerService defaultCandidateScannerService() {
        CandidateScannerService service = mock(CandidateScannerService.class);
        when(service.scanAllowedMarkets()).thenReturn(List.of());
        return service;
    }

    private TradingFlowResult flowResult(String market, SignalType signalType, boolean orderCreated, OrderStatus orderStatus) {
        return new TradingFlowResult(
                market,
                new BigDecimal("100"),
                signalType,
                "test",
                orderCreated,
                orderStatus,
                "Paper trading order filled",
                Instant.now()
        );
    }

    private TradingCandidate candidate(String market, CandidateDecision decision) {
        return new TradingCandidate(
                market,
                decision,
                decision == CandidateDecision.SELECTED ? "Long candidate selected" : "Trend is not UP",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("4.0"),
                new BigDecimal("10.0"),
                MarketTrend.UP,
                Instant.now()
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

    private StrategySelectionProperties strategySelectionProperties() {
        StrategySelectionProperties properties = mock(StrategySelectionProperties.class);
        when(properties.getStrategyName()).thenReturn("SimpleThresholdStrategy");
        return properties;
    }

    private TradingProperties tradingProperties() {
        TradingProperties properties = mock(TradingProperties.class);
        when(properties.getMaxOrderAmount()).thenReturn(new BigDecimal("100000"));
        when(properties.getAllowedMarkets()).thenReturn(List.of("KRW-BTC", "KRW-ETH"));
        return properties;
    }

    private PositionExitProperties positionExitProperties() {
        PositionExitProperties properties = mock(PositionExitProperties.class);
        when(properties.getTakeProfitRate()).thenReturn(new BigDecimal("5"));
        when(properties.getStopLossRate()).thenReturn(new BigDecimal("-3"));
        when(properties.isPositionExitEnabled()).thenReturn(false);
        return properties;
    }

    private DailyRiskProperties dailyRiskProperties() {
        DailyRiskProperties properties = mock(DailyRiskProperties.class);
        when(properties.isDailyRiskEnabled()).thenReturn(false);
        when(properties.getDailyOrderLimit()).thenReturn(10);
        when(properties.getDailyLossLimit()).thenReturn(new BigDecimal("50000"));
        return properties;
    }
}
