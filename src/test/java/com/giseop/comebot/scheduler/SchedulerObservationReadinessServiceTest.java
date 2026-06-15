package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.service.StrategyType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerObservationReadinessServiceTest {

    private final StrategySelectionProperties strategySelectionProperties = new StrategySelectionProperties();
    private final CandidateSchedulerProperties candidateSchedulerProperties = new CandidateSchedulerProperties();
    private final PositionExitSchedulerProperties positionExitSchedulerProperties = new PositionExitSchedulerProperties();
    private final SchedulerObservationReadinessService service = new SchedulerObservationReadinessService(
            strategySelectionProperties,
            candidateSchedulerProperties,
            positionExitSchedulerProperties
    );

    @Test
    void simpleThresholdHasNoSessionWarnings() {
        assertThat(service.candidateWarnings()).isEmpty();
    }

    @Test
    void sessionVolatilityWarnsForUnsafeInitialScope() {
        strategySelectionProperties.setSelected(StrategyType.SESSION_VOLATILITY_BREAKOUT);
        candidateSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setExchanges(List.of(ExchangeMode.UPBIT, ExchangeMode.BINANCE));
        candidateSchedulerProperties.setMarkets(List.of("KRW-BTC", "BTCUSDT"));
        candidateSchedulerProperties.setMaxBuysPerRun(2);
        positionExitSchedulerProperties.setEnabled(false);

        assertThat(service.candidateWarnings()).containsExactly(
                "SESSION_VOLATILITY_BREAKOUT should run candidate scheduler on BINANCE only",
                "SESSION_VOLATILITY_BREAKOUT candidate markets should be ALL_USDT or USDT symbols only",
                "Initial PAPER observation should use candidate maxBuysPerRun=1",
                "Exit scheduler should be ON when candidate scheduler is ON",
                "Exit scheduler exchanges should include BINANCE during Binance observation"
        );
    }

    @Test
    void sessionVolatilityAcceptsConstrainedObservationScope() {
        strategySelectionProperties.setSelected(StrategyType.SESSION_VOLATILITY_BREAKOUT);
        candidateSchedulerProperties.setEnabled(true);
        candidateSchedulerProperties.setExchanges(List.of(ExchangeMode.BINANCE));
        candidateSchedulerProperties.setMarkets(List.of(MarketSelectionService.ALL_USDT));
        candidateSchedulerProperties.setMaxBuysPerRun(1);
        positionExitSchedulerProperties.setEnabled(true);
        positionExitSchedulerProperties.setExchanges(List.of(ExchangeMode.BINANCE));

        assertThat(service.candidateWarnings()).isEmpty();
    }
}
