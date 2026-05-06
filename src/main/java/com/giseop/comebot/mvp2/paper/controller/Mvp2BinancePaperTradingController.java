package com.giseop.comebot.mvp2.paper.controller;

import com.giseop.comebot.mvp2.exchange.Exchange;
import com.giseop.comebot.mvp2.paper.Mvp2PaperCandidate;
import com.giseop.comebot.mvp2.paper.Mvp2PaperPortfolioSnapshot;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradeHistory;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingProperties;
import com.giseop.comebot.mvp2.paper.Mvp2PaperTradingService;
import com.giseop.comebot.mvp2.paper.dto.Mvp2PaperStatusResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Mvp2BinancePaperTradingController {

    private final Mvp2PaperTradingProperties properties;
    private final Mvp2PaperTradingService tradingService;

    public Mvp2BinancePaperTradingController(
            Mvp2PaperTradingProperties properties,
            Mvp2PaperTradingService tradingService
    ) {
        this.properties = properties;
        this.tradingService = tradingService;
    }

    @GetMapping("/api/mvp2/binance/paper/status")
    public Mvp2PaperStatusResponse status() {
        return new Mvp2PaperStatusResponse(
                properties.isBinanceSchedulerEnabled(),
                properties.getBinanceSchedulerFixedDelayMs(),
                properties.getBinanceSymbols(),
                properties.getInitialCash(),
                properties.getOrderAmount(),
                properties.getTakeProfitRate(),
                properties.getStopLossRate()
        );
    }

    @GetMapping("/api/mvp2/binance/paper/candidates")
    public List<Mvp2PaperCandidate> candidates() {
        return tradingService.scanBinanceCandidates();
    }

    @GetMapping("/api/mvp2/binance/paper/portfolio")
    public Mvp2PaperPortfolioSnapshot portfolio() {
        return tradingService.portfolio(Exchange.BINANCE);
    }

    @GetMapping("/api/mvp2/binance/paper/history")
    public List<Mvp2PaperTradeHistory> history(@RequestParam(defaultValue = "20") int limit) {
        return tradingService.history(Exchange.BINANCE, limit);
    }
}
