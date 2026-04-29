package com.giseop.comebot.risk.controller;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.dto.RiskStatusResponse;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskStatusController {

    private final TradingProperties tradingProperties;
    private final PositionExitProperties positionExitProperties;

    public RiskStatusController(
            TradingProperties tradingProperties,
            PositionExitProperties positionExitProperties
    ) {
        this.tradingProperties = tradingProperties;
        this.positionExitProperties = positionExitProperties;
    }

    @GetMapping("/api/risk/status")
    public RiskStatusResponse getStatus() {
        return new RiskStatusResponse(
                tradingProperties.getMaxOrderAmount(),
                new ArrayList<>(tradingProperties.getAllowedMarkets()),
                positionExitProperties.getTakeProfitRate(),
                positionExitProperties.getStopLossRate(),
                positionExitProperties.isPositionExitEnabled()
        );
    }
}
