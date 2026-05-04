package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.portfolio.PaperPortfolioProperties;
import com.giseop.comebot.portfolio.repository.InMemoryPaperPortfolioRepository;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.DailyRiskProperties;
import com.giseop.comebot.risk.service.DailyRiskValidationService;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StrategyExecutionFlowTest {

    @Test
    void buySignalCreatesOrderRequestAndPaperFillsAfterRiskValidation() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setBuyPrice(new BigDecimal("100"));
        strategyProperties.setSellPrice(new BigDecimal("200"));
        strategyProperties.setOrderQuantity(new BigDecimal("1"));
        strategyProperties.setOrderAmount(new BigDecimal("10000"));

        TradingSignal signal = new SimpleThresholdStrategy(strategyProperties)
                .evaluate(new MarketPrice("KRW-BTC", new BigDecimal("100"), Instant.now()));

        Optional<OrderRequest> request = new OrderRequestFactory().create(signal);

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));

        OrderExecutionService executionService = new OrderExecutionService(
                new PaperTradingExecutionGateway(),
                new RiskValidationService(tradingProperties),
                dailyRiskValidationService(paperPortfolioService()),
                paperPortfolioService()
        );

        OrderResult result = executionService.execute(request.orElseThrow());

        assertThat(result.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.market()).isEqualTo("KRW-BTC");
    }

    private PaperPortfolioService paperPortfolioService() {
        PaperPortfolioProperties properties = new PaperPortfolioProperties();
        properties.setInitialCash(new BigDecimal("1000000"));
        InMemoryPaperPortfolioRepository repository = new InMemoryPaperPortfolioRepository();
        PaperPortfolioService service = new PaperPortfolioService(repository, properties);
        service.initialize();
        return service;
    }

    private DailyRiskValidationService dailyRiskValidationService(PaperPortfolioService paperPortfolioService) {
        return new DailyRiskValidationService(
                new DailyRiskProperties(),
                new TradingFlowHistoryService(new InMemoryTradingFlowHistoryRepository()),
                paperPortfolioService
        );
    }
}
