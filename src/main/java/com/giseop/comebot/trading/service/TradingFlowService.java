package com.giseop.comebot.trading.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.TradingStrategy;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TradingFlowService {

    private final MarketPriceProvider marketPriceProvider;
    private final TradingStrategy tradingStrategy;
    private final OrderRequestFactory orderRequestFactory;
    private final OrderExecutionService orderExecutionService;

    public TradingFlowService(
            MarketPriceProvider marketPriceProvider,
            TradingStrategy tradingStrategy,
            OrderRequestFactory orderRequestFactory,
            OrderExecutionService orderExecutionService
    ) {
        this.marketPriceProvider = marketPriceProvider;
        this.tradingStrategy = tradingStrategy;
        this.orderRequestFactory = orderRequestFactory;
        this.orderExecutionService = orderExecutionService;
    }

    public TradingFlowResult run(String market) {
        MarketPrice marketPrice = marketPriceProvider.getCurrentPrice(market);
        TradingSignal signal = tradingStrategy.evaluate(marketPrice);
        Optional<OrderRequest> request = orderRequestFactory.create(signal);

        if (request.isEmpty()) {
            return new TradingFlowResult(
                    signal.market(),
                    signal.signalType(),
                    signal.reason(),
                    false,
                    null,
                    "No order created",
                    Instant.now()
            );
        }

        OrderResult orderResult = orderExecutionService.execute(request.get());
        return new TradingFlowResult(
                signal.market(),
                signal.signalType(),
                signal.reason(),
                true,
                orderResult.status(),
                orderResult.message(),
                orderResult.executedAt()
        );
    }
}
