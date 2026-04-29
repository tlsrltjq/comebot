package com.giseop.comebot.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.strategy.domain.SignalType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyRiskValidationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-04-29T10:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void validateApprovesWhenDailyRiskIsDisabled() {
        DailyRiskProperties properties = new DailyRiskProperties();

        assertThat(service(properties, List.of(), BigDecimal.ZERO).validate().decision())
                .isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void validateApprovesWhenFilledOrderCountIsBelowLimit() {
        DailyRiskProperties properties = enabledProperties();
        properties.setDailyOrderLimit(2);

        assertThat(service(properties, List.of(filledHistory("1")), BigDecimal.ZERO).validate().decision())
                .isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void validateRejectsWhenFilledOrderCountReachesLimit() {
        DailyRiskProperties properties = enabledProperties();
        properties.setDailyOrderLimit(2);

        assertThat(service(properties, List.of(filledHistory("1"), filledHistory("2")), BigDecimal.ZERO).validate())
                .satisfies(result -> {
                    assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
                    assertThat(result.reason()).isEqualTo("Daily order limit exceeded");
                });
    }

    @Test
    void validateExcludesHoldRejectedAndFailedFromOrderLimit() {
        DailyRiskProperties properties = enabledProperties();
        properties.setDailyOrderLimit(1);

        assertThat(service(properties, List.of(
                history("hold", false, null),
                history("rejected", true, OrderStatus.REJECTED),
                history("failed", true, OrderStatus.FAILED)
        ), BigDecimal.ZERO).validate().decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void validateRejectsWhenRealizedLossReachesLimit() {
        DailyRiskProperties properties = enabledProperties();
        properties.setDailyLossLimit(new BigDecimal("50000"));

        assertThat(service(properties, List.of(), new BigDecimal("50000")).validate())
                .satisfies(result -> {
                    assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
                    assertThat(result.reason()).isEqualTo("Daily loss limit exceeded");
                });
    }

    private DailyRiskValidationService service(
            DailyRiskProperties properties,
            List<TradingFlowHistory> histories,
            BigDecimal realizedLoss
    ) {
        TradingFlowHistoryService historyService = mock(TradingFlowHistoryService.class);
        when(historyService.findSince(Instant.parse("2026-04-28T15:00:00Z"))).thenReturn(histories);
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.realizedLossSince(Instant.parse("2026-04-28T15:00:00Z"))).thenReturn(realizedLoss);
        return new DailyRiskValidationService(properties, historyService, portfolioService, CLOCK);
    }

    private DailyRiskProperties enabledProperties() {
        DailyRiskProperties properties = new DailyRiskProperties();
        properties.setDailyRiskEnabled(true);
        properties.setDailyOrderLimit(10);
        properties.setDailyLossLimit(new BigDecimal("50000"));
        return properties;
    }

    private TradingFlowHistory filledHistory(String id) {
        return history(id, true, OrderStatus.FILLED);
    }

    private TradingFlowHistory history(String id, boolean orderCreated, OrderStatus orderStatus) {
        return new TradingFlowHistory(
                id,
                "KRW-BTC",
                new BigDecimal("100"),
                orderStatus == null ? SignalType.HOLD : SignalType.BUY,
                "test",
                orderCreated,
                orderStatus,
                "test",
                Instant.parse("2026-04-29T00:00:00Z")
        );
    }
}
