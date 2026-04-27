package com.giseop.comebot.execution.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaperTradingExecutionGatewayTest {

    private final PaperTradingExecutionGateway gateway = new PaperTradingExecutionGateway();

    @Test
    void executeReturnsFilledResultForValidOrder() {
        OrderRequest request = new OrderRequest(
                "KRW-BTC",
                OrderSide.BUY,
                new BigDecimal("0.01"),
                new BigDecimal("100000000"),
                Instant.now()
        );

        OrderResult result = gateway.execute(request);

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.market()).isEqualTo("KRW-BTC");
        assertThat(result.side()).isEqualTo(OrderSide.BUY);
        assertThat(result.quantity()).isEqualByComparingTo("0.01");
        assertThat(result.price()).isEqualByComparingTo("100000000");
        assertThat(result.executedAt()).isNotNull();
    }

    @Test
    void executeRejectsInvalidQuantity() {
        OrderRequest request = new OrderRequest(
                "KRW-BTC",
                OrderSide.BUY,
                BigDecimal.ZERO,
                new BigDecimal("100000000"),
                Instant.now()
        );

        OrderResult result = gateway.execute(request);

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Quantity must be greater than zero");
    }

    @Test
    void executeRejectsInvalidPrice() {
        OrderRequest request = new OrderRequest(
                "KRW-BTC",
                OrderSide.SELL,
                new BigDecimal("0.01"),
                BigDecimal.ZERO,
                Instant.now()
        );

        OrderResult result = gateway.execute(request);

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Price must be greater than zero");
    }
}
