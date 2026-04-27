package com.giseop.comebot.strategy.controller;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.strategy.dto.StrategyStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StrategyStatusController {

    private static final String STRATEGY_NAME = "SimpleThresholdStrategy";

    private final StrategyProperties strategyProperties;

    public StrategyStatusController(StrategyProperties strategyProperties) {
        this.strategyProperties = strategyProperties;
    }

    @GetMapping("/api/strategy/status")
    public StrategyStatusResponse getStatus() {
        return new StrategyStatusResponse(
                STRATEGY_NAME,
                strategyProperties.getBuyPrice(),
                strategyProperties.getSellPrice(),
                strategyProperties.getOrderQuantity()
        );
    }
}
