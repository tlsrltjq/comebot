package com.giseop.comebot.strategy.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.CandidateExecutionService;
import com.giseop.comebot.strategy.dto.TradingCandidateResponse;
import com.giseop.comebot.trading.dto.TradingFlowRunResponse;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CandidateController {

    private final CandidateScannerService candidateScannerService;
    private final CandidateExecutionService candidateExecutionService;

    public CandidateController(
            CandidateScannerService candidateScannerService,
            CandidateExecutionService candidateExecutionService
    ) {
        this.candidateScannerService = candidateScannerService;
        this.candidateExecutionService = candidateExecutionService;
    }

    @GetMapping("/api/candidates")
    public ResponseEntity<List<TradingCandidateResponse>> getCandidates(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        ExchangeModeResolver.requireImplemented(exchangeMode);

        if (market != null && market.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (market != null) {
            return ResponseEntity.ok(List.of(TradingCandidateResponse.from(candidateScannerService.scan(market))));
        }

        List<TradingCandidateResponse> response = candidateScannerService.scanAllowedMarkets().stream()
                .map(TradingCandidateResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/candidates/execute")
    public ResponseEntity<TradingFlowRunResponse> executeCandidate(@RequestParam String market) {
        if (market == null || market.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        TradingFlowResult result = candidateExecutionService.execute(market);
        return ResponseEntity.ok(new TradingFlowRunResponse(
                result.market(),
                result.signalType(),
                result.signalReason(),
                result.orderCreated(),
                result.orderStatus(),
                result.message(),
                result.executedAt()
        ));
    }
}
