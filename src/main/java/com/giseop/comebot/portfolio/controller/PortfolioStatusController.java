package com.giseop.comebot.portfolio.controller;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.exchange.ExchangeModeResolver;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioStatusResponse;
import com.giseop.comebot.portfolio.dto.PortfolioValuationFailureResponse;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.dto.PositionResponse;
import com.giseop.comebot.portfolio.dto.SelectedPaperSellRequest;
import com.giseop.comebot.portfolio.dto.SelectedPaperSellResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.portfolio.service.PaperPortfolioValuationService;
import com.giseop.comebot.portfolio.service.SelectedPaperSellService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioStatusController {

    private final PaperPortfolioService paperPortfolioService;
    private final PaperPortfolioValuationService paperPortfolioValuationService;
    private final SelectedPaperSellService selectedPaperSellService;

    public PortfolioStatusController(
            PaperPortfolioService paperPortfolioService,
            PaperPortfolioValuationService paperPortfolioValuationService,
            SelectedPaperSellService selectedPaperSellService
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.paperPortfolioValuationService = paperPortfolioValuationService;
        this.selectedPaperSellService = selectedPaperSellService;
    }

    @GetMapping("/api/portfolio/status")
    public ResponseEntity<PortfolioStatusResponse> getStatus(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);

        PaperPortfolio portfolio = paperPortfolioService.getPortfolio(exchangeMode);
        return ResponseEntity.ok(new PortfolioStatusResponse(
                portfolio.exchange().name(),
                portfolio.currency(),
                portfolio.cash(),
                portfolio.realizedProfit()
        ));
    }

    @GetMapping("/api/portfolio/positions")
    public ResponseEntity<List<PositionResponse>> getPositions(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);

        return ResponseEntity.ok(paperPortfolioService.findPositions(exchangeMode).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/api/portfolio/valuation")
    public ResponseEntity<?> getValuation(@RequestParam(required = false) String exchange) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);

        try {
            PortfolioValuationResponse response = paperPortfolioValuationService.valuate(exchangeMode);
            return ResponseEntity.ok(response);
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new PortfolioValuationFailureResponse("Portfolio valuation failed"));
        }
    }

    private PositionResponse toResponse(PaperPosition position) {
        return new PositionResponse(position.market(), position.quantity(), position.averageBuyPrice());
    }

    @PostMapping("/api/portfolio/positions/sell-selected")
    public ResponseEntity<SelectedPaperSellResponse> sellSelected(
            @RequestParam(required = false) String exchange,
            @RequestBody SelectedPaperSellRequest request
    ) {
        ExchangeMode exchangeMode = ExchangeModeResolver.resolve(exchange);
        if (request == null || request.markets() == null || request.markets().stream().noneMatch(market -> market != null && !market.isBlank())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(selectedPaperSellService.sellSelected(exchangeMode, request.markets()));
    }
}
