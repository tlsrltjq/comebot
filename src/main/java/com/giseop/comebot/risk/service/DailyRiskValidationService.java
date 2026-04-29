package com.giseop.comebot.risk.service;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DailyRiskValidationService {

    private final DailyRiskProperties dailyRiskProperties;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final PaperPortfolioService paperPortfolioService;
    private final Clock clock;

    @Autowired
    public DailyRiskValidationService(
            DailyRiskProperties dailyRiskProperties,
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioService paperPortfolioService
    ) {
        this(dailyRiskProperties, tradingFlowHistoryService, paperPortfolioService, Clock.systemDefaultZone());
    }

    DailyRiskValidationService(
            DailyRiskProperties dailyRiskProperties,
            TradingFlowHistoryService tradingFlowHistoryService,
            PaperPortfolioService paperPortfolioService,
            Clock clock
    ) {
        this.dailyRiskProperties = dailyRiskProperties;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.paperPortfolioService = paperPortfolioService;
        this.clock = clock;
    }

    public RiskCheckResult validate() {
        Instant checkedAt = Instant.now(clock);
        if (!dailyRiskProperties.isDailyRiskEnabled()) {
            return new RiskCheckResult(RiskDecision.APPROVED, "Daily risk disabled", checkedAt);
        }

        Instant todayStart = todayStart();
        long filledOrderCount = tradingFlowHistoryService.findSince(todayStart).stream()
                .filter(history -> history.orderCreated() && history.orderStatus() == OrderStatus.FILLED)
                .count();
        if (filledOrderCount >= dailyRiskProperties.getDailyOrderLimit()) {
            return new RiskCheckResult(RiskDecision.REJECTED, "Daily order limit exceeded", checkedAt);
        }

        BigDecimal realizedLoss = paperPortfolioService.realizedLossSince(todayStart);
        if (realizedLoss.compareTo(dailyRiskProperties.getDailyLossLimit()) >= 0) {
            return new RiskCheckResult(RiskDecision.REJECTED, "Daily loss limit exceeded", checkedAt);
        }

        return new RiskCheckResult(RiskDecision.APPROVED, "Daily risk approved", checkedAt);
    }

    private Instant todayStart() {
        ZoneId zoneId = clock.getZone();
        return LocalDate.now(clock).atStartOfDay(zoneId).toInstant();
    }
}
