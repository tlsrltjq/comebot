package com.giseop.comebot.strategy.controller;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.StrategySelectionProperties;
import com.giseop.comebot.strategy.dto.StrategyStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StrategyStatusController {

    private final StrategyProperties strategyProperties;
    private final StrategySelectionProperties strategySelectionProperties;

    public StrategyStatusController(
            StrategyProperties strategyProperties,
            StrategySelectionProperties strategySelectionProperties
    ) {
        this.strategyProperties = strategyProperties;
        this.strategySelectionProperties = strategySelectionProperties;
    }

    @GetMapping("/api/strategy/status")
    public StrategyStatusResponse getStatus() {
        return new StrategyStatusResponse(
                strategySelectionProperties.getStrategyName(),
                strategyProperties.getBuyPrice(),
                strategyProperties.getSellPrice(),
                strategyProperties.getOrderQuantity()
        );
    }
}
