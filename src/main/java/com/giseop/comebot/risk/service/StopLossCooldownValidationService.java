package com.giseop.comebot.risk.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.risk.StopLossCooldownProperties;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.strategy.domain.SignalType;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StopLossCooldownValidationService {

    private static final String STOP_LOSS_REASON = "Stop loss";

    private final StopLossCooldownProperties properties;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final Clock clock;

    @Autowired
    public StopLossCooldownValidationService(
            StopLossCooldownProperties properties,
            TradingFlowHistoryService tradingFlowHistoryService
    ) {
        this(properties, tradingFlowHistoryService, Clock.systemUTC());
    }

    StopLossCooldownValidationService(
            StopLossCooldownProperties properties,
            TradingFlowHistoryService tradingFlowHistoryService,
            Clock clock
    ) {
        this.properties = properties;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.clock = clock;
    }

    public RiskCheckResult validate(ExchangeMode exchange, OrderRequest request) {
        Instant checkedAt = Instant.now(clock);
        if (!properties.isEnabled()) {
            return approved(checkedAt);
        }
        if (request == null || request.side() != OrderSide.BUY || request.market() == null || request.market().isBlank()) {
            return approved(checkedAt);
        }

        Instant windowStart = checkedAt.minus(properties.getWindow());
        List<TradingFlowHistory> stopLosses = tradingFlowHistoryService.findSince(exchange, windowStart).stream()
                .filter(history -> request.market().equals(history.market()))
                .filter(this::isFilledStopLossSell)
                .toList();
        if (stopLosses.size() < properties.getTriggerCount()) {
            return approved(checkedAt);
        }

        Instant latestStopLossAt = stopLosses.stream()
                .map(TradingFlowHistory::createdAt)
                .filter(createdAt -> createdAt != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
        if (latestStopLossAt == null || !latestStopLossAt.plus(properties.getDuration()).isAfter(checkedAt)) {
            return approved(checkedAt);
        }

        return new RiskCheckResult(
                RiskDecision.REJECTED,
                "Stop loss cooldown active: market=%s stopLossCount=%d cooldownUntil=%s".formatted(
                        request.market(),
                        stopLosses.size(),
                        latestStopLossAt.plus(properties.getDuration())
                ),
                checkedAt
        );
    }

    private boolean isFilledStopLossSell(TradingFlowHistory history) {
        return history != null
                && history.signalType() == SignalType.SELL
                && history.orderStatus() == OrderStatus.FILLED
                && history.signalReason() != null
                && history.signalReason().contains(STOP_LOSS_REASON);
    }

    private RiskCheckResult approved(Instant checkedAt) {
        return new RiskCheckResult(RiskDecision.APPROVED, "Stop loss cooldown approved", checkedAt);
    }
}
