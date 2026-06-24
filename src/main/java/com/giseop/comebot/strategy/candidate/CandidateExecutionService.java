package com.giseop.comebot.strategy.candidate;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.PendingLimitOrderService;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.domain.SignalType;
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
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final CandidateScanLogService candidateScanLogService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final KillSwitchService killSwitchService;
    private final PositionEntryGuardService positionEntryGuardService;
    private final PendingLimitOrderService pendingLimitOrderService;

    public CandidateExecutionService(
            CandidateScannerService candidateScannerService,
            StrategyMarketSettingsService strategyMarketSettingsService,
            TradingFlowHistoryService tradingFlowHistoryService,
            CandidateScanLogService candidateScanLogService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService,
            TradingFlowNotificationService tradingFlowNotificationService,
            KillSwitchService killSwitchService,
            PositionEntryGuardService positionEntryGuardService,
            PendingLimitOrderService pendingLimitOrderService
    ) {
        this.candidateScannerService = candidateScannerService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.candidateScanLogService = candidateScanLogService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.killSwitchService = killSwitchService;
        this.positionEntryGuardService = positionEntryGuardService;
        this.pendingLimitOrderService = pendingLimitOrderService;
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
        candidateScanLogService.save(exchange, candidate);
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
        if (positionEntryGuardService.shouldBlockEntry(exchange, candidate.market(), candidate.currentPrice())) {
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

        if (pendingLimitOrderService.hasPending(exchange, candidate.market())) {
            return save(new TradingFlowResult(
                    candidate.market(),
                    candidate.currentPrice(),
                    SignalType.HOLD,
                    "Limit order already pending",
                    false,
                    null,
                    "Candidate entry blocked by pending limit order",
                    candidate.scannedAt()
            ), exchange);
        }

        java.math.BigDecimal limitPrice = candidate.currentPrice();
        java.math.BigDecimal quantity = strategyMarketSettingsService.buyQuantity(candidate.market(), limitPrice);
        if (!pendingLimitOrderService.tryPlace(exchange, candidate.market(), limitPrice, quantity, candidate.reason())) {
            return save(new TradingFlowResult(
                    candidate.market(),
                    candidate.currentPrice(),
                    SignalType.HOLD,
                    "Limit order already pending",
                    false,
                    null,
                    "Candidate entry blocked by pending limit order",
                    candidate.scannedAt()
            ), exchange);
        }
        return save(new TradingFlowResult(
                candidate.market(),
                limitPrice,
                SignalType.BUY,
                candidate.reason(),
                true,
                OrderStatus.REQUESTED,
                "Limit order placed at " + limitPrice,
                Instant.now()
        ), exchange);
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
