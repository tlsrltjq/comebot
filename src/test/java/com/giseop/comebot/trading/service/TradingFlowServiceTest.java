package com.giseop.comebot.trading.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.config.TradingProperties;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.gateway.PaperTradingExecutionGateway;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.market.provider.InMemoryMarketPriceProvider;
import com.giseop.comebot.risk.service.RiskValidationService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.SimpleThresholdStrategy;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradingFlowServiceTest {

    private InMemoryMarketPriceProvider marketPriceProvider;
    private TradingFlowService tradingFlowService;

    @BeforeEach
    void setUp() {
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setBuyPrice(new BigDecimal("100"));
        strategyProperties.setSellPrice(new BigDecimal("200"));
        strategyProperties.setOrderQuantity(new BigDecimal("1"));

        TradingProperties tradingProperties = new TradingProperties();
        tradingProperties.setMaxOrderAmount(new BigDecimal("100000"));
        tradingProperties.setAllowedMarkets(List.of("KRW-BTC", "KRW-ETH"));

        marketPriceProvider = new InMemoryMarketPriceProvider();
        tradingFlowService = new TradingFlowService(
                marketPriceProvider,
                new SimpleThresholdStrategy(strategyProperties),
                new OrderRequestFactory(),
                new OrderExecutionService(
                        new PaperTradingExecutionGateway(),
                        new RiskValidationService(tradingProperties)
                )
        );
    }

    @Test
    void runFillsPaperBuyOrderWhenPriceIsAtOrBelowBuyThreshold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runFillsPaperSellOrderWhenPriceIsAtOrAboveSellThreshold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("200"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.SELL);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void runDoesNotExecuteOrderWhenSignalIsHold() {
        marketPriceProvider.updatePrice("KRW-BTC", new BigDecimal("150"));

        TradingFlowResult result = tradingFlowService.run("KRW-BTC");

        assertThat(result.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderStatus()).isNull();
        assertThat(result.message()).isEqualTo("No order created");
    }

    @Test
    void runReturnsRejectedOrderResultWhenMarketIsNotAllowed() {
        marketPriceProvider.updatePrice("KRW-XRP", new BigDecimal("100"));

        TradingFlowResult result = tradingFlowService.run("KRW-XRP");

        assertThat(result.signalType()).isEqualTo(SignalType.BUY);
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.message()).isEqualTo("Market is not allowed");
    }
}
