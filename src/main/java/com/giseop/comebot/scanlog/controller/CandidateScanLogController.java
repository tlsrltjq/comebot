package com.giseop.comebot.scanlog.controller;

import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.dto.CandidateScanLogResponse;
import com.giseop.comebot.scanlog.service.CandidateScanLogService;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidate-scan-log")
public class CandidateScanLogController {

    private final CandidateScanLogService candidateScanLogService;

    public CandidateScanLogController(CandidateScanLogService candidateScanLogService) {
        this.candidateScanLogService = candidateScanLogService;
    }

    @GetMapping
    public ResponseEntity<List<CandidateScanLogResponse>> findSince(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(defaultValue = "UPBIT") ExchangeMode exchange,
            @RequestParam(required = false) CandidateDecision decision
    ) {
        Instant from = Instant.now().minus(AnalyticsRange.from(range).duration());
        List<CandidateScanLogResponse> result = candidateScanLogService
                .findSince(exchange, from, decision)
                .stream()
                .map(CandidateScanLogResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
