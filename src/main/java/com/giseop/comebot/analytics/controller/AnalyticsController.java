package com.giseop.comebot.analytics.controller;

import com.giseop.comebot.analytics.dto.AnalyticsLossResponse;
import com.giseop.comebot.analytics.dto.AnalyticsPnlResponse;
import com.giseop.comebot.analytics.dto.AnalyticsRange;
import com.giseop.comebot.analytics.dto.AnalyticsSummaryResponse;
import com.giseop.comebot.analytics.dto.MatchedTrade;
import com.giseop.comebot.analytics.service.AnalyticsService;
import com.giseop.comebot.analytics.service.MatchedTradeService;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MatchedTradeService matchedTradeService;

    public AnalyticsController(AnalyticsService analyticsService, MatchedTradeService matchedTradeService) {
        this.analyticsService = analyticsService;
        this.matchedTradeService = matchedTradeService;
    }

    @GetMapping("/api/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> summary(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        return ResponseEntity.ok(analyticsService.summary(AnalyticsRange.from(range), exchangeMode));
    }

    @GetMapping("/api/analytics/pnl")
    public ResponseEntity<AnalyticsPnlResponse> pnl(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        return ResponseEntity.ok(analyticsService.pnl(AnalyticsRange.from(range), exchangeMode));
    }

    @GetMapping("/api/analytics/losses")
    public ResponseEntity<AnalyticsLossResponse> losses(
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        return ResponseEntity.ok(analyticsService.losses(AnalyticsRange.from(range), exchangeMode));
    }

    @GetMapping("/api/analytics/matched-trades")
    public ResponseEntity<List<MatchedTrade>> matchedTrades(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String exchange
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        return ResponseEntity.ok(matchedTradeService.findRecent(exchangeMode, limit));
    }
}
