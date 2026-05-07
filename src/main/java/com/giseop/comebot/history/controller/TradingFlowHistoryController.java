package com.giseop.comebot.history.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.history.domain.TradingFlowHistory;
import com.giseop.comebot.history.dto.TradingFlowHistoryResponse;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingFlowHistoryController {

    private final TradingFlowHistoryService tradingFlowHistoryService;

    public TradingFlowHistoryController(TradingFlowHistoryService tradingFlowHistoryService) {
        this.tradingFlowHistoryService = tradingFlowHistoryService;
    }

    @GetMapping("/api/trading-flow/history")
    public ResponseEntity<List<TradingFlowHistoryResponse>> findRecent(
            @RequestParam(required = false) String market,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        ExchangeModeResolver.requireImplemented(exchangeMode);

        if (limit <= 0 || (market != null && market.isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        List<TradingFlowHistoryResponse> response = tradingFlowHistoryService.findRecent(market, limit).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/trading-flow/history/{id}")
    public ResponseEntity<TradingFlowHistoryResponse> findById(@PathVariable String id) {
        return tradingFlowHistoryService.findById(id)
                .map(history -> ResponseEntity.ok(toResponse(history)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TradingFlowHistoryResponse toResponse(TradingFlowHistory history) {
        return new TradingFlowHistoryResponse(
                history.id(),
                history.market(),
                history.currentPrice(),
                history.signalType(),
                history.signalReason(),
                history.orderCreated(),
                history.orderStatus(),
                history.message(),
                history.createdAt()
        );
    }
}
