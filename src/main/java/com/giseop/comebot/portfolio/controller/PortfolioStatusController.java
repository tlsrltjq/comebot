package com.giseop.comebot.portfolio.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioStatusResponse;
import com.giseop.comebot.portfolio.dto.PortfolioValuationFailureResponse;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.dto.PositionResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioStatusController {

    private final PaperPortfolioService paperPortfolioService;
    private final PaperPortfolioValuationService paperPortfolioValuationService;

    public PortfolioStatusController(
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.paperPortfolioValuationService = paperPortfolioValuationService;
    }

    @GetMapping("/api/portfolio/status")
    public ResponseEntity<PortfolioStatusResponse> getStatus(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        ExchangeModeResolver.requireImplemented(exchangeMode);

        PaperPortfolio portfolio = paperPortfolioService.getPortfolio();
        return ResponseEntity.ok(new PortfolioStatusResponse(portfolio.cash(), portfolio.realizedProfit()));
    }

    @GetMapping("/api/portfolio/positions")
    public ResponseEntity<List<PositionResponse>> getPositions(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        ExchangeModeResolver.requireImplemented(exchangeMode);

        return ResponseEntity.ok(paperPortfolioService.findPositions().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/api/portfolio/valuation")
    public ResponseEntity<?> getValuation(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        ExchangeModeResolver.requireImplemented(exchangeMode);

        try {
            PortfolioValuationResponse response = paperPortfolioValuationService.valuate();
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new PortfolioValuationFailureResponse("Portfolio valuation failed"));
        }
    }

    private PositionResponse toResponse(PaperPosition position) {
        return new PositionResponse(position.market(), position.quantity(), position.averageBuyPrice());
    }
}
