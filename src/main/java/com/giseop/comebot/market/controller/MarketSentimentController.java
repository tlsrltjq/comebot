package com.giseop.comebot.market.controller;

import com.giseop.comebot.market.dto.MarketSentimentSnapshot;
import com.giseop.comebot.market.service.MarketSentimentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketSentimentController {

    private final MarketSentimentService marketSentimentService;

    public MarketSentimentController(MarketSentimentService marketSentimentService) {
        this.marketSentimentService = marketSentimentService;
    }

    @GetMapping("/sentiment")
    public MarketSentimentSnapshot sentiment() {
        return marketSentimentService.latest();
    }
}
