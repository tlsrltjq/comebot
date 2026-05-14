package com.giseop.comebot.telegram.inbound;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
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
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.scheduler.CandidateSchedulerProperties;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.safety.SafetyProperties;
import com.giseop.comebot.scheduler.TradingSchedulerProperties;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.telegram.TelegramProperties;
import com.giseop.comebot.telegram.sender.TelegramApiClient;
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
    private final TelegramApiClient telegramApiClient;
    private final DatabaseHealthService databaseHealthService;
    private final MarketPriceProviderProperties marketPriceProviderProperties;
    private final StrategyProperties strategyProperties;
    private final StrategySelectionProperties strategySelectionProperties;
    private final TradingProperties tradingProperties;
    private final TelegramProperties telegramProperties;
    private final TelegramInboundProperties telegramInboundProperties;
    private final NotificationProperties notificationProperties;
    private final TradingSchedulerProperties tradingSchedulerProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final SafetyProperties safetyProperties;
    private final PositionExitProperties positionExitProperties;
    private final DailyRiskProperties dailyRiskProperties;
    private final TradingFlowService tradingFlowService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final PaperPortfolioService paperPortfolioService;
    private final PaperPortfolioValuationService paperPortfolioValuationService;
    private final CandidateScannerService candidateScannerService;
    private final CandidateExecutionService candidateExecutionService;

    public TelegramCommandService(
            TelegramCommandParser commandParser,
            TelegramCallbackParser callbackParser,
            TelegramNotificationSender telegramNotificationSender,
            TelegramApiClient telegramApiClient,
            DatabaseHealthService databaseHealthService,
            MarketPriceProviderProperties marketPriceProviderProperties,
            StrategyProperties strategyProperties,
            StrategySelectionProperties strategySelectionProperties,
            TradingProperties tradingProperties,
            TelegramProperties telegramProperties,
            TelegramInboundProperties telegramInboundProperties,
            NotificationProperties notificationProperties,
            TradingSchedulerProperties tradingSchedulerProperties,
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            SafetyProperties safetyProperties,
            PositionExitProperties positionExitProperties,
            DailyRiskProperties dailyRiskProperties,
            TradingFlowService tradingFlowService,
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService,
            CandidateScannerService candidateScannerService,
            CandidateExecutionService candidateExecutionService
    ) {
        this.commandParser = commandParser;
        this.callbackParser = callbackParser;
        this.telegramNotificationSender = telegramNotificationSender;
        this.telegramApiClient = telegramApiClient;
        this.databaseHealthService = databaseHealthService;
        this.marketPriceProviderProperties = marketPriceProviderProperties;
        this.strategyProperties = strategyProperties;
        this.strategySelectionProperties = strategySelectionProperties;
        this.tradingProperties = tradingProperties;
        this.telegramProperties = telegramProperties;
        this.telegramInboundProperties = telegramInboundProperties;
        this.notificationProperties = notificationProperties;
        this.tradingSchedulerProperties = tradingSchedulerProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.safetyProperties = safetyProperties;
        this.positionExitProperties = positionExitProperties;
        this.dailyRiskProperties = dailyRiskProperties;
        this.tradingFlowService = tradingFlowService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.paperPortfolioService = paperPortfolioService;
        this.paperPortfolioValuationService = paperPortfolioValuationService;
        this.candidateScannerService = candidateScannerService;
        this.candidateExecutionService = candidateExecutionService;
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
            case AUTO -> autoMessage();
            case PNL -> pnlMessage();
            case CONDITIONS -> conditionsMessage();
            case CANDIDATES -> candidatesMessage();
            case CANDIDATE_RUN -> candidateRunMessage(command.market());
            case RUN -> runMessage(command.market());
            case HISTORY -> historyMessage(command.market());
            case PORTFOLIO -> portfolioMessage();
            case POSITIONS -> positionsMessage();
            case RISK -> riskMessage();
            case SAFETY -> safetyMessage();
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
            case AUTO -> autoMessage();
            case PNL -> pnlMessage();
            case CONDITIONS -> conditionsMessage();
            case CANDIDATES -> candidatesMessage();
            case CANDIDATE_RUN -> candidateRunMessage(callback.market());
            case RUN -> runMessage(callback.market());
            case HISTORY -> historyMessage(callback.market());
            case PORTFOLIO -> portfolioMessage();
            case POSITIONS -> positionsMessage();
            case RISK -> riskMessage();
            case SAFETY -> safetyMessage();
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
                "메뉴에서 조회할 항목을 선택하세요.",
                TelegramInlineKeyboard.mainMenu()
        );
    }

    private void sendText(String response) {
        telegramNotificationSender.sendMessage(new NotificationMessage("Telegram command response", response, Instant.now()));
    }

    private String helpMessage() {
        return """
                사용 가능한 명령:
                /help - 도움말
                /menu - 버튼 메뉴
                /status - 시스템 상태
                /auto - 자동매매 상태
                /conditions - 현재 매매 조건
                /pnl - 손익 요약
                /candidates - 롱 후보 조회
                /candidate-run, /run - 비활성화됨
                /history KRW-BTC - 실행 이력
                /portfolio - 포트폴리오 요약
                /positions - 보유 포지션
                /risk - 리스크 설정
                /safety - 안전장치 상태
                """.trim();
    }

    private String statusMessage() {
        return """
                시스템 상태
                DB 연결: %s
                시세 Provider: %s
                전략(Strategy): %s
                1회 거래(Order): %s KRW
                최대 주문 금액(Max order amount): %s
                허용 마켓(Allowed markets): %s
                전략 스케줄러(Trading Scheduler): %s
                후보 스케줄러(Candidate Scheduler): %s
                후보 거래소(Candidate exchange): %s
                청산 스케줄러(Exit Scheduler): %s
                청산 거래소(Exit exchange): %s
                수동 PAPER 실행(Manual paper run): 차단(Blocked)
                긴급 정지(Kill switch): %s
                알림(Notifications): %s
                후보 요약 알림(Candidate summary): %s
                텔레그램(Telegram): %s
                텔레그램 수신(Inbound): %s
                """.formatted(
                databaseHealthService.check().connected(),
                marketPriceProviderProperties.getPriceProvider(),
                strategySelectionProperties.getStrategyName(),
                strategyProperties.getOrderAmount(),
                tradingProperties.getMaxOrderAmount(),
                tradingProperties.getAllowedMarkets(),
                enabled(tradingSchedulerProperties.isEnabled()),
                enabled(candidateSchedulerProperties.isEnabled()),
                candidateSchedulerProperties.getExchange(),
                enabled(positionExitSchedulerProperties.isEnabled()),
                positionExitSchedulerProperties.getExchange(),
                enabled(safetyProperties.isKillSwitchEnabled()),
                enabled(notificationProperties.isEnabled()),
                enabled(candidateSchedulerProperties.isNotifySummary()),
                enabled(telegramProperties.isEnabled()),
                enabled(telegramInboundProperties.isEnabled())
        ).trim();
    }

    private String autoMessage() {
        return """
                자동매매 상태
                전략 스케줄러(Trading Scheduler): %s
                전략 주기(Trading interval): %s ms
                전략 대상(Trading markets): %s
                후보 스케줄러(Candidate Scheduler): %s
                후보 거래소(Candidate exchange): %s
                후보 주기(Candidate interval): %s ms
                후보 대상(Candidate markets): %s
                후보 요약 알림(Candidate summary): %s
                청산 스케줄러(Exit Scheduler): %s
                청산 거래소(Exit exchange): %s
                청산 주기(Exit interval): %s ms
                청산 HOLD 기록(Exit HOLD history): %s
                수동 PAPER 실행(Manual paper run): 차단(Blocked)
                """.formatted(
                enabled(tradingSchedulerProperties.isEnabled()),
                tradingSchedulerProperties.getFixedDelayMs(),
                tradingSchedulerProperties.getMarkets(),
                enabled(candidateSchedulerProperties.isEnabled()),
                candidateSchedulerProperties.getExchange(),
                candidateSchedulerProperties.getFixedDelayMs(),
                candidateSchedulerProperties.getMarkets(),
                enabled(candidateSchedulerProperties.isNotifySummary()),
                enabled(positionExitSchedulerProperties.isEnabled()),
                positionExitSchedulerProperties.getExchange(),
                positionExitSchedulerProperties.getFixedDelayMs(),
                positionExitSchedulerProperties.isSaveHoldHistory() ? "저장(Save)" : "저장 안 함(Skip)"
        ).trim();
    }

    private String conditionsMessage() {
        return """
                매매 조건(Trading Conditions)
                거래 모드(Trading mode): PAPER_TRADING
                전략(Strategy): %s
                허용 마켓(Allowed markets): %s
                후보 범위(Candidate universe): 전체 KRW 중 24시간 거래대금 상위 50개
                현재가 수집(Price polling): 1초 fixedDelay
                전략 스케줄러 주기(Trading interval): %s ms
                후보 스케줄러 주기(Candidate interval): %s ms
                청산 스케줄러 주기(Exit interval): %s ms
                1회 거래(Order amount): %s KRW
                BUY 추세: UP만 허용
                익절(Take profit): %s
                손절(Stop loss): %s
                실제 주문 API(Real order API): 없음
                """.formatted(
                strategySelectionProperties.getStrategyName(),
                tradingProperties.getAllowedMarkets(),
                tradingSchedulerProperties.getFixedDelayMs(),
                candidateSchedulerProperties.getFixedDelayMs(),
                positionExitSchedulerProperties.getFixedDelayMs(),
                strategyProperties.getOrderAmount(),
                positionExitProperties.getTakeProfitRate(),
                positionExitProperties.getStopLossRate()
        ).trim();
    }

    private String pnlMessage() {
        try {
            PortfolioValuationResponse valuation = paperPortfolioValuationService.valuate();
            return """
                    손익 요약(PnL Summary)
                    현금(Cash): %s
                    총 평가금(Total Equity): %s
                    실현 손익(Realized): %s
                    미실현 손익(Unrealized): %s
                    총 손익(Total PnL): %s
                    """.formatted(
                    valuation.cash(),
                    valuation.totalEquity(),
                    valuation.realizedProfit(),
                    valuation.unrealizedProfit(),
                    valuation.totalProfit()
            ).trim();
        } catch (RuntimeException e) {
            return "손익 평가 실패: 현재가를 가져올 수 없습니다.";
        }
    }

    private String candidatesMessage() {
        List<TradingCandidate> candidates = candidateScannerService.scanAllowedMarkets();
        if (candidates.isEmpty()) {
            return "롱 후보가 없습니다.";
        }

        StringBuilder builder = new StringBuilder("롱 후보 목록");
        for (TradingCandidate candidate : candidates) {
            builder.append(System.lineSeparator())
                    .append("- ")
                    .append(candidate.market())
                    .append(" | 판단: ")
                    .append(toKoreanDecision(candidate))
                    .append(" | 현재가: ")
                    .append(candidate.currentPrice())
                    .append(" | 변동률: ")
                    .append(candidate.priceChangeRate())
                    .append("% | 거래대금 변화: ")
                    .append(candidate.tradeAmountChangeRate())
                    .append("% | 추세: ")
                    .append(candidate.trend())
                    .append(System.lineSeparator())
                    .append("  사유: ")
                    .append(candidate.reason());
        }
        return builder.toString();
    }

    private String candidateRunMessage(String market) {
        return manualExecutionDisabledMessage();
    }

    private String runMessage(String market) {
        return manualExecutionDisabledMessage();
    }

    private String manualExecutionDisabledMessage() {
        return "텔레그램 수동 PAPER 실행은 코드 레벨에서 차단되어 있습니다. 자동 실행 결과는 /auto, /history, /portfolio, /pnl로 확인하세요.";
    }

    private String historyMessage(String market) {
        if (market == null || market.isBlank()) {
            return "사용법: /history KRW-BTC";
        }

        List<TradingFlowHistory> histories = tradingFlowHistoryService.findRecent(market, HISTORY_LIMIT);
        if (histories.isEmpty()) {
            return "해당 market의 이력이 없습니다. market=%s".formatted(market);
        }

        StringBuilder builder = new StringBuilder("최근 실행 이력 market=").append(market);
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
                    PAPER 포트폴리오(Portfolio)
                    현금(Cash): %s
                    총 평가금(Total Equity): %s
                    실현 손익(Realized): %s
                    미실현 손익(Unrealized): %s
                    총 손익(Total PnL): %s
                    """.formatted(
                    valuation.cash(),
                    valuation.totalEquity(),
                    valuation.realizedProfit(),
                    valuation.unrealizedProfit(),
                    valuation.totalProfit()
            ).trim();
        } catch (RuntimeException e) {
            return "포트폴리오 평가 실패: 현재가를 가져올 수 없습니다.";
        }
    }

    private String positionsMessage() {
        List<PaperPosition> positions = paperPortfolioService.findPositions();
        if (positions.isEmpty()) {
            return "보유 포지션이 없습니다.";
        }

        StringBuilder builder = new StringBuilder("보유 포지션(Positions)");
        for (PaperPosition position : positions) {
            builder.append(System.lineSeparator())
                    .append("- 마켓(Market): ")
                    .append(position.market())
                    .append(" | 수량(Quantity): ")
                    .append(position.quantity())
                    .append(" | 평균매수가(Avg Buy): ")
                    .append(position.averageBuyPrice());
        }
        return builder.toString();
    }

    private String riskMessage() {
        return """
                리스크 정책(Risk Policy)
                최대 주문 금액(Max order amount): %s
                허용 마켓(Allowed markets): %s
                익절(Take profit): %s
                손절(Stop loss): %s
                청산 평가(Position exit): %s
                일일 리스크(Daily risk): %s
                일일 주문 한도(Daily order limit): %s
                일일 손실 한도(Daily loss limit): %s
                """.formatted(
                tradingProperties.getMaxOrderAmount(),
                tradingProperties.getAllowedMarkets(),
                positionExitProperties.getTakeProfitRate(),
                positionExitProperties.getStopLossRate(),
                enabled(positionExitProperties.isPositionExitEnabled()),
                enabled(dailyRiskProperties.isDailyRiskEnabled()),
                dailyRiskProperties.getDailyOrderLimit(),
                dailyRiskProperties.getDailyLossLimit()
        ).trim();
    }

    private String safetyMessage() {
        return """
                안전장치(Safety)
                긴급 정지(Kill switch): %s
                """.formatted(enabled(safetyProperties.isKillSwitchEnabled())).trim();
    }

    private String toKoreanDecision(TradingCandidate candidate) {
        return switch (candidate.decision()) {
            case SELECTED -> "선택";
            case SKIPPED -> "제외";
        };
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String enabled(boolean value) {
        return value ? "켜짐(Enabled)" : "꺼짐(Disabled)";
    }

}
