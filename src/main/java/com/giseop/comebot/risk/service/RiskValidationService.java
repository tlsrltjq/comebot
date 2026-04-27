package com.giseop.comebot.risk.service;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class RiskValidationService {

    private final TradingProperties tradingProperties;

    public RiskValidationService(TradingProperties tradingProperties) {
        this.tradingProperties = tradingProperties;
    }

    public RiskCheckResult validate(OrderRequest request) {
        if (request == null) {
            return rejected("Order request must not be null");
        }
        if (request.market() == null || request.market().isBlank()) {
            return rejected("Market must not be blank");
        }
        if (request.side() == null) {
            return rejected("Order side must not be null");
        }
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return rejected("Quantity must be greater than zero");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            return rejected("Price must be greater than zero");
        }
        if (!tradingProperties.getAllowedMarkets().contains(request.market())) {
            return rejected("Market is not allowed");
        }

        BigDecimal orderAmount = request.quantity().multiply(request.price());
        if (orderAmount.compareTo(tradingProperties.getMaxOrderAmount()) > 0) {
            return rejected("Order amount exceeds max order amount");
        }

        return new RiskCheckResult(RiskDecision.APPROVED, "Risk check approved", Instant.now());
    }

    private RiskCheckResult rejected(String reason) {
        return new RiskCheckResult(RiskDecision.REJECTED, reason, Instant.now());
    }
}
