package com.giseop.comebot.trading.controller;

import com.giseop.comebot.trading.dto.TradingFlowRunResponse;
import com.giseop.comebot.trading.service.TradingFlowResult;
import com.giseop.comebot.trading.service.TradingFlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingFlowController {

    private final TradingFlowService tradingFlowService;

    public TradingFlowController(TradingFlowService tradingFlowService) {
        this.tradingFlowService = tradingFlowService;
    }

    @GetMapping("/api/trading-flow/run")
    public ResponseEntity<TradingFlowRunResponse> run(@RequestParam String market) {
        if (market == null || market.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TradingFlowResult result = tradingFlowService.run(market);
        return ResponseEntity.ok(toResponse(result));
    }

    private TradingFlowRunResponse toResponse(TradingFlowResult result) {
        return new TradingFlowRunResponse(
                result.market(),
                result.signalType(),
                result.signalReason(),
                result.orderCreated(),
                result.orderStatus(),
                result.message(),
                result.executedAt()
        );
    }
}
