package com.giseop.comebot.market.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.market.dto.BtcChangeChartResponse;
import com.giseop.comebot.market.service.BtcChangeChartService;
import com.giseop.comebot.market.service.BtcChangeRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MarketOverviewController {

    private final BtcChangeChartService btcChangeChartService;

    public MarketOverviewController(BtcChangeChartService btcChangeChartService) {
        this.btcChangeChartService = btcChangeChartService;
    }

    @GetMapping("/api/market/btc-change")
    public ResponseEntity<BtcChangeChartResponse> getBtcChange(
            @RequestParam(required = false) String exchange,
            @RequestParam(defaultValue = "24h") String range
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        BtcChangeRange btcRange = resolveRange(range);
        return ResponseEntity.ok(btcChangeChartService.chart(exchangeMode, btcRange));
    }

    private BtcChangeRange resolveRange(String range) {
        try {
            return BtcChangeRange.from(range);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
