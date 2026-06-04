package com.giseop.comebot.market.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.market.dto.MarketFlowSummary;
import com.giseop.comebot.market.service.MarketFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketFlowController {

    private final MarketFlowService marketFlowService;

    public MarketFlowController(MarketFlowService marketFlowService) {
        this.marketFlowService = marketFlowService;
    }

    @GetMapping("/fund-flow")
    public MarketFlowSummary fundFlow(
            @RequestParam(defaultValue = "UPBIT") String exchange
    ) {
        ExchangeMode mode = ExchangeMode.valueOf(exchange.toUpperCase());
        return marketFlowService.summary(mode);
    }
}
