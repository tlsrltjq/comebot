package com.giseop.comebot.analytics.controller;

import com.giseop.comebot.analytics.dto.AnalyticsLossResponse;
import com.giseop.comebot.analytics.dto.AnalyticsPnlResponse;
import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.analytics.dto.AnalyticsSummaryResponse;
import com.giseop.comebot.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> summary(@RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(analyticsService.summary(AnalyticsRange.from(range)));
    }

    @GetMapping("/api/analytics/pnl")
    public ResponseEntity<AnalyticsPnlResponse> pnl(@RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(analyticsService.pnl(AnalyticsRange.from(range)));
    }

    @GetMapping("/api/analytics/losses")
    public ResponseEntity<AnalyticsLossResponse> losses(@RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(analyticsService.losses(AnalyticsRange.from(range)));
    }
}
