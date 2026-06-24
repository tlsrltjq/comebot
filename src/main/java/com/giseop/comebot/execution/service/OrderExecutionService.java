package com.giseop.comebot.execution.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.domain.PendingLimitOrder;
import com.giseop.comebot.execution.gateway.ExecutionGateway;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.risk.service.DailyRiskValidationService;
import com.giseop.comebot.risk.service.RiskValidationService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OrderExecutionService {

    private final ExecutionGateway executionGateway;
    private final RiskValidationService riskValidationService;
    private final DailyRiskValidationService dailyRiskValidationService;
    private final PaperPortfolioService paperPortfolioService;

    public OrderExecutionService(
            ExecutionGateway executionGateway,
            RiskValidationService riskValidationService,
            DailyRiskValidationService dailyRiskValidationService,
            PaperPortfolioService paperPortfolioService
    ) {
        if (executionGateway == null || !executionGateway.supportsOnlyPaperTrading()) {
            throw new UnsupportedOperationException("Only PAPER_TRADING execution gateways are supported");
        }
        this.executionGateway = executionGateway;
        this.riskValidationService = riskValidationService;
        this.dailyRiskValidationService = dailyRiskValidationService;
        this.paperPortfolioService = paperPortfolioService;
    }

    public OrderResult execute(OrderRequest request) {
        return execute(ExchangeMode.UPBIT, request);
    }

    public OrderResult execute(ExchangeMode exchange, OrderRequest request) {
        RiskCheckResult riskCheckResult = riskValidationService.validate(exchange, request);
        if (riskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, riskCheckResult);
        }
        RiskCheckResult dailyRiskCheckResult = dailyRiskValidationService.validate(exchange);
        if (dailyRiskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, dailyRiskCheckResult);
        }
        return paperPortfolioService.validate(exchange, request)
                .map(reason -> rejected(request, reason))
                .orElseGet(() -> executeAndApply(exchange, request));
    }

    public OrderResult executePaperPositionExit(ExchangeMode exchange, OrderRequest request) {
        String validationMessage = validatePaperExitRequest(exchange, request);
        if (validationMessage != null) {
            return rejected(request, validationMessage);
        }
        return paperPortfolioService.validate(exchange, request)
                .map(reason -> rejected(request, reason))
                .orElseGet(() -> executeAndApply(exchange, request));
    }

    private OrderResult rejected(OrderRequest request, RiskCheckResult riskCheckResult) {
        return rejected(request, riskCheckResult.reason(), riskCheckResult.checkedAt());
    }

    private OrderResult rejected(OrderRequest request, String reason) {
        return rejected(request, reason, java.time.Instant.now());
    }

    private OrderResult rejected(OrderRequest request, String reason, java.time.Instant rejectedAt) {
        return new OrderResult(
                request == null ? null : request.market(),
                request == null ? null : request.side(),
                request == null ? null : request.quantity(),
                request == null ? null : request.price(),
                OrderStatus.REJECTED,
                reason,
                rejectedAt
        );
    }

    private String validatePaperExitRequest(ExchangeMode exchange, OrderRequest request) {
        if (request == null) {
            return "Order request must not be null";
        }
        if (request.side() != OrderSide.SELL) {
            return "Paper position exit only supports SELL orders";
        }
        if (request.market() == null || request.market().isBlank()) {
            return "Market must not be blank";
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            return "Quantity must be greater than zero";
        }
        if (request.price() == null || request.price().signum() <= 0) {
            return "Price must be greater than zero";
        }
        if (exchange == ExchangeMode.BINANCE && !request.market().endsWith("USDT")) {
            return "Binance PAPER orders only support USDT spot symbols";
        }
        return null;
    }

    public OrderResult fillLimitOrder(ExchangeMode exchange, PendingLimitOrder order) {
        OrderRequest request = new OrderRequest(
                order.market(),
                OrderSide.BUY,
                order.quantity(),
                order.limitPrice(),
                Instant.now()
        );
        RiskCheckResult riskCheckResult = riskValidationService.validate(exchange, request);
        if (riskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, riskCheckResult);
        }
        RiskCheckResult dailyRiskCheckResult = dailyRiskValidationService.validate(exchange);
        if (dailyRiskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, dailyRiskCheckResult);
        }
        return paperPortfolioService.validate(exchange, request)
                .map(reason -> rejected(request, reason))
                .orElseGet(() -> executeAndApply(exchange, request));
    }

    private OrderResult executeAndApply(ExchangeMode exchange, OrderRequest request) {
        OrderResult result = executionGateway.execute(request);
        paperPortfolioService.apply(exchange, result);
        return result;
    }
}
