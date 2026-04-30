package com.giseop.comebot.strategy.controller;

import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.dto.TradingCandidateResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CandidateController {

    private final CandidateScannerService candidateScannerService;

    public CandidateController(CandidateScannerService candidateScannerService) {
        this.candidateScannerService = candidateScannerService;
    }

    @GetMapping("/api/candidates")
    public ResponseEntity<List<TradingCandidateResponse>> getCandidates(
            @RequestParam(required = false) String market
    ) {
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
}
