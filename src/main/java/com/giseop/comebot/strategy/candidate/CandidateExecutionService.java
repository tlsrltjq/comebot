package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.PositionEntryGuardService;
import com.giseop.comebot.strategy.service.StrategyMarketSettingsService;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CandidateExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CandidateExecutionService.class);

    private final CandidateScannerService candidateScannerService;
    private final StrategyMarketSettingsService strategyMarketSettingsService;
    private final OrderRequestFactory orderRequestFactory;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final KillSwitchService killSwitchService;
    private final PositionEntryGuardService positionEntryGuardService;

    public CandidateExecutionService(
            CandidateScannerService candidateScannerService,
            StrategyMarketSettingsService strategyMarketSettingsService,
            OrderRequestFactory orderRequestFactory,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService,
            TradingFlowNotificationService tradingFlowNotificationService,
            KillSwitchService killSwitchService,
            PositionEntryGuardService positionEntryGuardService
    ) {
        this.candidateScannerService = candidateScannerService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
        this.orderRequestFactory = orderRequestFactory;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.killSwitchService = killSwitchService;
        this.positionEntryGuardService = positionEntryGuardService;
    }

    public TradingFlowResult execute(String market) {
        return execute(ExchangeMode.UPBIT, market);
    }

    public TradingFlowResult execute(ExchangeMode exchange, String market) {
        if (killSwitchService.isEnabled()) {
            return save(new TradingFlowResult(
                    market,
                    null,
                    null,
                    "Kill switch enabled",
                    false,
                    OrderStatus.REJECTED,
                    "Kill switch enabled: candidate execution blocked",
                    Instant.now()
            ), exchange);
        }

        TradingCandidate candidate = candidateScannerService.scan(exchange, market);
        if (candidate.decision() != CandidateDecision.SELECTED) {
            return save(new TradingFlowResult(
                    candidate.market(),
                    candidate.currentPrice(),
                    SignalType.HOLD,
                    candidate.reason(),
                    false,
                    null,
                    "Candidate was not selected",
                    candidate.scannedAt()
            ), exchange);
        }
        if (positionEntryGuardService.shouldBlockEntry(exchange, candidate.market())) {
            return save(new TradingFlowResult(
                    candidate.market(),
                    candidate.currentPrice(),
                    SignalType.HOLD,
                    "Paper position already exists",
                    false,
                    null,
                    "Candidate entry blocked by existing paper position",
                    candidate.scannedAt()
            ), exchange);
        }

        TradingSignal signal = new TradingSignal(
                candidate.market(),
                SignalType.BUY,
                candidate.reason(),
                candidate.currentPrice(),
                strategyMarketSettingsService.buyQuantity(candidate.market(), candidate.currentPrice()),
                candidate.scannedAt()
        );

        return orderRequestFactory.create(signal)
                .map(request -> {
                    OrderResult orderResult = orderExecutionService.execute(exchange, request);
                    return save(new TradingFlowResult(
                            candidate.market(),
                            candidate.currentPrice(),
                            SignalType.BUY,
                            candidate.reason(),
                            true,
                            orderResult.status(),
                            orderResult.message(),
                            orderResult.executedAt()
                    ), exchange);
                })
                .orElseGet(() -> save(new TradingFlowResult(
                        candidate.market(),
                        candidate.currentPrice(),
                        SignalType.HOLD,
                        candidate.reason(),
                        false,
                        null,
                        "No order created",
                        Instant.now()
                ), exchange));
    }

    private TradingFlowResult save(TradingFlowResult result) {
        return save(result, ExchangeMode.UPBIT);
    }

    private TradingFlowResult save(TradingFlowResult result, ExchangeMode exchange) {
        tradingFlowHistoryService.save(exchange, result);
        notifyIfEnabled(result);
        return result;
    }

    private void notifyIfEnabled(TradingFlowResult result) {
        if (!notificationProperties.isEnabled() || !notificationPolicyService.shouldNotify(result)) {
            return;
        }

        try {
            tradingFlowNotificationService.notify(result);
        } catch (RuntimeException exception) {
            log.warn("Candidate execution notification failed. market={}", result.market(), exception);
        }
    }
}
