package com.giseop.comebot.risk.controller;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.risk.ConcentrationRiskProperties;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.StopLossCooldownProperties;
import com.giseop.comebot.risk.dto.RiskStatusResponse;
import java.util.ArrayList;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RiskStatusController {

    private final TradingProperties tradingProperties;
    private final PositionExitProperties positionExitProperties;
    private final DailyRiskProperties dailyRiskProperties;
    private final ConcentrationRiskProperties concentrationRiskProperties;
    private final StopLossCooldownProperties stopLossCooldownProperties;

    public RiskStatusController(
            TradingProperties tradingProperties,
            PositionExitProperties positionExitProperties,
            DailyRiskProperties dailyRiskProperties,
            ConcentrationRiskProperties concentrationRiskProperties,
            StopLossCooldownProperties stopLossCooldownProperties
    ) {
        this.tradingProperties = tradingProperties;
        this.positionExitProperties = positionExitProperties;
        this.dailyRiskProperties = dailyRiskProperties;
        this.concentrationRiskProperties = concentrationRiskProperties;
        this.stopLossCooldownProperties = stopLossCooldownProperties;
    }

    @GetMapping("/api/risk/status")
    public RiskStatusResponse getStatus(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        return new RiskStatusResponse(
                tradingProperties.getMaxOrderAmount(),
                new ArrayList<>(tradingProperties.getAllowedMarkets()),
                positionExitProperties.getTakeProfitRate(),
                positionExitProperties.getStopLossRate(),
                positionExitProperties.isPositionExitEnabled(),
                dailyRiskProperties.isDailyRiskEnabled(),
                dailyRiskProperties.getDailyOrderLimit(),
                dailyRiskProperties.getDailyLossLimit(),
                new RiskStatusResponse.ConcentrationStatus(
                        exchangeMode.name(),
                        concentrationRiskProperties.isEnabled(),
                        concentrationRiskProperties.warningExposureRate(exchangeMode),
                        concentrationRiskProperties.blockExposureRate(exchangeMode)
                ),
                new RiskStatusResponse.StopLossCooldownStatus(
                        stopLossCooldownProperties.isEnabled(),
                        stopLossCooldownProperties.getWindow().toString(),
                        stopLossCooldownProperties.getTriggerCount(),
                        stopLossCooldownProperties.getDuration().toString()
                )
        );
    }
}
