package com.giseop.comebot.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.ExecutionGateway;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.service.RiskValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderExecutionServiceTest {

    private CountingExecutionGateway executionGateway;
    private OrderExecutionService orderExecutionService;

    @BeforeEach
    void setUp() {
        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));

        executionGateway = new CountingExecutionGateway();
        orderExecutionService = new OrderExecutionService(
                executionGateway,
                new RiskValidationService(tradingProperties),
                paperPortfolioService()
        );
    }

    @Test
    void executeFillsOrderAfterRiskApproval() {
        OrderResult result = orderExecutionService.execute(orderRequest("KRW-BTC", "0.01", "5000000"));

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(executionGateway.callCount).isEqualTo(1);
    }

    @Test
    void executeRejectsOrderWhenAmountExceedsMaxOrderAmount() {
        OrderResult result = orderExecutionService.execute(orderRequest("KRW-BTC", "1", "100001"));

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Order amount exceeds max order amount");
        assertThat(executionGateway.callCount).isZero();
    }

    @Test
    void executeRejectsOrderWhenMarketIsNotAllowed() {
        OrderResult result = orderExecutionService.execute(orderRequest("KRW-XRP", "1", "1000"));

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Market is not allowed");
        assertThat(executionGateway.callCount).isZero();
    }

    @Test
    void executeDoesNotCallGatewayWhenRiskValidationFails() {
        OrderRequest request = new OrderRequest(
                "KRW-BTC",
                OrderSide.BUY,
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                Instant.now()
        );

        OrderResult result = orderExecutionService.execute(request);

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Quantity must be greater than zero");
        assertThat(executionGateway.callCount).isZero();
    }

    @Test
    void executeRejectsNullOrderRequest() {
        OrderResult result = orderExecutionService.execute(null);

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Order request must not be null");
        assertThat(executionGateway.callCount).isZero();
    }

    @Test
    void executeRejectsBuyWhenPaperCashIsNotEnough() {
        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("2000000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));
        orderExecutionService = new OrderExecutionService(
                executionGateway,
                new RiskValidationService(tradingProperties),
                paperPortfolioService()
        );

        OrderResult result = orderExecutionService.execute(orderRequest("KRW-BTC", "1", "1000001"));

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Paper cash is not enough");
        assertThat(executionGateway.callCount).isZero();
    }

    @Test
    void executeRejectsSellWhenPaperPositionQuantityIsNotEnough() {
        OrderResult result = orderExecutionService.execute(new OrderRequest(
                "KRW-BTC",
                OrderSide.SELL,
                new BigDecimal("1"),
                new BigDecimal("1000"),
                Instant.now()
        ));

        assertThat(result.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Paper position quantity is not enough");
        assertThat(executionGateway.callCount).isZero();
    }

    private OrderRequest orderRequest(String market, String quantity, String price) {
        return new OrderRequest(
                market,
                OrderSide.BUY,
                new BigDecimal(quantity),
                new BigDecimal(price),
                Instant.now()
        );
    }

    private PaperPortfolioService paperPortfolioService() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        InMemoryPaperPortfolioRepository repository = new InMemoryPaperPortfolioRepository();
        PaperPortfolioService service = new PaperPortfolioService(repository, properties);
        service.initialize();
        return service;
    }

    private static class CountingExecutionGateway implements ExecutionGateway {

        private int callCount;

        @Override
        public OrderResult execute(OrderRequest request) {
            callCount++;
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
    }
}
