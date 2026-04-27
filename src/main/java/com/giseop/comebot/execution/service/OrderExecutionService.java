package com.giseop.comebot.execution.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.ExecutionGateway;
import com.giseop.comebot.risk.domain.RiskCheckResult;
import com.giseop.comebot.risk.domain.RiskDecision;
import com.giseop.comebot.risk.service.RiskValidationService;
import org.springframework.stereotype.Service;

@Service
public class OrderExecutionService {

    private final ExecutionGateway executionGateway;
    private final RiskValidationService riskValidationService;

    public OrderExecutionService(
            ExecutionGateway executionGateway,
            RiskValidationService riskValidationService
    ) {
        this.executionGateway = executionGateway;
        this.riskValidationService = riskValidationService;
    }

    public OrderResult execute(OrderRequest request) {
        RiskCheckResult riskCheckResult = riskValidationService.validate(request);
        if (riskCheckResult.decision() == RiskDecision.REJECTED) {
            return rejected(request, riskCheckResult);
        }
        return executionGateway.execute(request);
    }

    private OrderResult rejected(OrderRequest request, RiskCheckResult riskCheckResult) {
        return new OrderResult(
                request == null ? null : request.market(),
                request == null ? null : request.side(),
                request == null ? null : request.quantity(),
                request == null ? null : request.price(),
                OrderStatus.REJECTED,
                riskCheckResult.reason(),
                riskCheckResult.checkedAt()
        );
    }
}
