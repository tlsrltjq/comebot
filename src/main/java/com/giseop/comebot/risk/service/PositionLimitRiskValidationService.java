package com.giseop.comebot.risk.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionLimitProperties;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class PositionLimitRiskValidationService {

    private final PositionLimitProperties properties;
    private final PaperPortfolioService paperPortfolioService;

    public PositionLimitRiskValidationService(
            PositionLimitProperties properties,
            PaperPortfolioService paperPortfolioService
    ) {
        this.properties = properties;
        this.paperPortfolioService = paperPortfolioService;
    }

    public RiskCheckResult validate(ExchangeMode exchange, OrderRequest request) {
        if (!properties.isEnabled() || request == null || request.side() != OrderSide.BUY) {
            return approved();
        }
        if (paperPortfolioService.findPosition(exchange, request.market())
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .isPresent()) {
            return approved();
        }

        int exchangePositionCount = paperPortfolioService.findPositions(exchange).size();
        int totalPositionCount = paperPortfolioService.findPositions(ExchangeMode.UPBIT).size()
                + paperPortfolioService.findPositions(ExchangeMode.BINANCE).size();
        int exchangeLimit = properties.exchangeMaxPositions(exchange);
        if (exchangePositionCount >= exchangeLimit) {
            return rejected("Exchange position limit reached: exchange=%s positions=%d limit=%d".formatted(
                    exchange == null ? ExchangeMode.UPBIT : exchange,
                    exchangePositionCount,
                    exchangeLimit
            ));
        }
        if (totalPositionCount >= properties.getTotalMaxPositions()) {
            return rejected("Total position limit reached: positions=%d limit=%d".formatted(
                    totalPositionCount,
                    properties.getTotalMaxPositions()
            ));
        }
        return approved();
    }

    private RiskCheckResult approved() {
        return new RiskCheckResult(RiskDecision.APPROVED, "Position limit risk check approved", Instant.now());
    }

    private RiskCheckResult rejected(String reason) {
        return new RiskCheckResult(RiskDecision.REJECTED, reason, Instant.now());
    }
}
