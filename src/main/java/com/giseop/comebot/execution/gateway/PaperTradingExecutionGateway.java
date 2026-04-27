package com.giseop.comebot.execution.gateway;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PaperTradingExecutionGateway implements ExecutionGateway {

    @Override
    public OrderResult execute(OrderRequest request) {
        if (request == null) {
            return failed("Order request must not be null");
        }

        String validationMessage = validate(request);
        if (validationMessage != null) {
            return new OrderResult(
                    request.market(),
                    request.side(),
                    request.quantity(),
                    request.price(),
                    OrderStatus.REJECTED,
                    validationMessage,
                    Instant.now()
            );
        }

        return new OrderResult(
                request.market(),
                request.side(),
                request.quantity(),
                request.price(),
                OrderStatus.FILLED,
                "Paper trading order filled",
                Instant.now()
        );
    }

    private String validate(OrderRequest request) {
        if (request.market() == null || request.market().isBlank()) {
            return "Market must not be blank";
        }
        if (request.side() == null) {
            return "Order side must not be null";
        }
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return "Quantity must be greater than zero";
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            return "Price must be greater than zero";
        }
        return null;
    }

    private OrderResult failed(String message) {
        return new OrderResult(
                null,
                null,
                null,
                null,
                OrderStatus.FAILED,
                message,
                Instant.now()
        );
    }
}
