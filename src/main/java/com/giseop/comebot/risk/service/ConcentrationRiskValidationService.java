package com.giseop.comebot.risk.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.ConcentrationRiskProperties;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ConcentrationRiskValidationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ConcentrationRiskProperties properties;
    private final PaperPortfolioService paperPortfolioService;

    public ConcentrationRiskValidationService(
            ConcentrationRiskProperties properties,
            PaperPortfolioService paperPortfolioService
    ) {
        this.properties = properties;
        this.paperPortfolioService = paperPortfolioService;
    }

    public RiskCheckResult validate(ExchangeMode exchange, OrderRequest request) {
        if (!properties.isEnabled()) {
            return approved();
        }
        if (request == null || request.side() != OrderSide.BUY) {
            return approved();
        }

        PaperPortfolio portfolio = paperPortfolioService.getPortfolio(exchange);
        BigDecimal currentMarketCost = portfolio.positions().stream()
                .filter(position -> request.market().equals(position.market()))
                .map(this::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPositionCost = portfolio.positions().stream()
                .map(this::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderAmount = request.quantity().multiply(request.price());
        BigDecimal estimatedEquity = portfolio.cash().add(totalPositionCost);
        if (estimatedEquity.compareTo(BigDecimal.ZERO) <= 0) {
            return approved();
        }

        BigDecimal nextExposureRate = currentMarketCost.add(orderAmount)
                .multiply(HUNDRED)
                .divide(estimatedEquity, 4, RoundingMode.HALF_UP);
        BigDecimal blockRate = properties.blockExposureRate(exchange);
        if (nextExposureRate.compareTo(blockRate) >= 0) {
            return new RiskCheckResult(
                    RiskDecision.REJECTED,
                    "Market concentration exceeds block exposure rate: market=%s exposure=%s%% limit=%s%%".formatted(
                            request.market(),
                            nextExposureRate.stripTrailingZeros().toPlainString(),
                            blockRate.stripTrailingZeros().toPlainString()
                    ),
                    Instant.now()
            );
        }
        return approved();
    }

    private RiskCheckResult approved() {
        return new RiskCheckResult(RiskDecision.APPROVED, "Concentration risk check approved", Instant.now());
    }

    private BigDecimal costBasis(PaperPosition position) {
        if (position.quantity() == null || position.averageBuyPrice() == null) {
            return BigDecimal.ZERO;
        }
        return position.quantity().multiply(position.averageBuyPrice());
    }
}
