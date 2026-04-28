package com.giseop.comebot.portfolio.controller;

import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioStatusResponse;
import com.giseop.comebot.portfolio.dto.PositionResponse;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioStatusController {

    private final PaperPortfolioService paperPortfolioService;

    public PortfolioStatusController(PaperPortfolioService paperPortfolioService) {
        this.paperPortfolioService = paperPortfolioService;
    }

    @GetMapping("/api/portfolio/status")
    public PortfolioStatusResponse getStatus() {
        PaperPortfolio portfolio = paperPortfolioService.getPortfolio();
        return new PortfolioStatusResponse(portfolio.cash(), portfolio.realizedProfit());
    }

    @GetMapping("/api/portfolio/positions")
    public List<PositionResponse> getPositions() {
        return paperPortfolioService.findPositions().stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionResponse toResponse(PaperPosition position) {
        return new PositionResponse(position.market(), position.quantity(), position.averageBuyPrice());
    }
}
