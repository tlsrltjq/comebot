package com.giseop.comebot.scheduler;

import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.service.MarketSelectionService;
import com.giseop.comebot.strategy.service.StrategyEntryProperties;
import com.giseop.comebot.strategy.service.StrategyType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SchedulerObservationReadinessService {

    private final StrategySelectionProperties strategySelectionProperties;
    private final CandidateSchedulerProperties candidateSchedulerProperties;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final StrategyEntryProperties strategyEntryProperties;

    public SchedulerObservationReadinessService(
            StrategySelectionProperties strategySelectionProperties,
            CandidateSchedulerProperties candidateSchedulerProperties,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            StrategyEntryProperties strategyEntryProperties
    ) {
        this.strategySelectionProperties = strategySelectionProperties;
        this.candidateSchedulerProperties = candidateSchedulerProperties;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.strategyEntryProperties = strategyEntryProperties;
    }

    public List<String> candidateWarnings() {
        if (strategySelectionProperties.getSelected() != StrategyType.SESSION_VOLATILITY_BREAKOUT) {
            return List.of();
        }

        List<String> warnings = new ArrayList<>();
        if (!candidateSchedulerProperties.getExchanges().equals(List.of(ExchangeMode.BINANCE))) {
            warnings.add("SESSION_VOLATILITY_BREAKOUT should run candidate scheduler on BINANCE only");
        }
        if (hasNonBinanceMarket(candidateSchedulerProperties.getMarkets())) {
            warnings.add("SESSION_VOLATILITY_BREAKOUT candidate markets should be ALL_USDT or USDT symbols only");
        }
        if (candidateSchedulerProperties.getMaxBuysPerRun() == 0
                || candidateSchedulerProperties.getMaxBuysPerRun() > 1) {
            warnings.add("Initial PAPER observation should use candidate maxBuysPerRun=1");
        }
        if (!strategyEntryProperties.isPreventReentryWithPosition()) {
            warnings.add("SESSION_VOLATILITY_BREAKOUT should prevent re-entry while a PAPER position exists");
        }
        if (candidateSchedulerProperties.isEnabled() && !positionExitSchedulerProperties.isEnabled()) {
            warnings.add("Exit scheduler should be ON when candidate scheduler is ON");
        }
        if (candidateSchedulerProperties.isEnabled()
                && !positionExitSchedulerProperties.getExchanges().contains(ExchangeMode.BINANCE)) {
            warnings.add("Exit scheduler exchanges should include BINANCE during Binance observation");
        }
        return List.copyOf(warnings);
    }

    private boolean hasNonBinanceMarket(List<String> markets) {
        return markets.stream()
                .filter(market -> market != null && !market.isBlank())
                .map(String::trim)
                .anyMatch(market -> !MarketSelectionService.ALL_USDT.equalsIgnoreCase(market)
                        && !market.toUpperCase(java.util.Locale.ROOT).endsWith("USDT"));
    }
}
