package com.giseop.comebot.strategy.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.strategy.candidate.CandidateScanCache;
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
    private final CandidateScanCache candidateScanCache;
    private static final int DEFAULT_FULL_SCAN_LIMIT = 20;
    private static final int MAX_FULL_SCAN_LIMIT = 50;

    public CandidateController(
            CandidateScannerService candidateScannerService,
            CandidateExecutionService candidateExecutionService,
            CandidateScanCache candidateScanCache
    ) {
        this.candidateScannerService = candidateScannerService;
        this.candidateExecutionService = candidateExecutionService;
        this.candidateScanCache = candidateScanCache;
    }

    @GetMapping("/api/candidates")
    public ResponseEntity<List<TradingCandidateResponse>> getCandidates(
            @RequestParam(required = false) String market,
            @RequestParam(required = false) String exchange,
            @RequestParam(defaultValue = "" + DEFAULT_FULL_SCAN_LIMIT) int limit,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);

        if (market != null && market.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (market != null) {
            return ResponseEntity.ok(List.of(TradingCandidateResponse.from(candidateScannerService.scan(exchangeMode, market))));
        }

        int normalizedLimit = normalizeLimit(limit);
        return ResponseEntity.ok(candidateScanCache.getOrLoad(
                exchangeMode,
                normalizedLimit,
                refresh,
                () -> scanAllowedMarkets(exchangeMode, normalizedLimit)
        ));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_FULL_SCAN_LIMIT;
        }
        return Math.min(limit, MAX_FULL_SCAN_LIMIT);
    }

    private List<TradingCandidateResponse> scanAllowedMarkets(ExchangeMode exchangeMode, int limit) {
        return candidateScannerService.scanAllowedMarkets(exchangeMode, limit).stream()
                .map(TradingCandidateResponse::from)
                .toList();
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
