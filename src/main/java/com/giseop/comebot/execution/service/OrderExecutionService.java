package com.giseop.comebot.execution.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.ExecutionGateway;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.risk.service.RiskValidationService;
import org.springframework.stereotype.Service;

@Service
public class OrderExecutionService {

    private final ExecutionGateway executionGateway;
    private final RiskValidationService riskValidationService;
    private final PaperPortfolioService paperPortfolioService;

    public OrderExecutionService(
            ExecutionGateway executionGateway,
            RiskValidationService riskValidationService,
            PaperPortfolioService paperPortfolioService
    ) {
        this.executionGateway = executionGateway;
        this.riskValidationService = riskValidationService;
        this.paperPortfolioService = paperPortfolioService;
    }

    public OrderResult execute(OrderRequest request) {
        RiskCheckResult riskCheckResult = riskValidationService.validate(request);
        if (riskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, riskCheckResult);
        }
        return paperPortfolioService.validate(request)
                .map(reason -> rejected(request, reason))
                .orElseGet(() -> executeAndApply(request));
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

    private OrderResult executeAndApply(OrderRequest request) {
        OrderResult result = executionGateway.execute(request);
        paperPortfolioService.apply(result);
        return result;
    }
}
