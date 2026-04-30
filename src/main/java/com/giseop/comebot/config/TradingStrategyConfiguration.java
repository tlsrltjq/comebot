package com.giseop.comebot.config;

import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.service.SimpleThresholdStrategy;
import com.giseop.comebot.strategy.service.TradingStrategy;
import com.giseop.comebot.strategy.service.VolatilityBreakoutLongStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradingStrategyConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "strategy", name = "selected", havingValue = "SIMPLE_THRESHOLD", matchIfMissing = true)
    public TradingStrategy simpleThresholdStrategy(StrategyProperties strategyProperties) {
        return new SimpleThresholdStrategy(strategyProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "strategy", name = "selected", havingValue = "VOLATILITY_BREAKOUT_LONG")
    public TradingStrategy volatilityBreakoutLongStrategy(
            CandidateScannerService candidateScannerService,
            StrategyProperties strategyProperties,
            com.giseop.comebot.strategy.service.PositionEntryGuardService positionEntryGuardService
    ) {
        return new VolatilityBreakoutLongStrategy(candidateScannerService, strategyProperties, positionEntryGuardService);
    }
}
