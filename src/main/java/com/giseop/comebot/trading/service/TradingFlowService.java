package com.giseop.comebot.trading.service;

import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import com.giseop.comebot.strategy.service.TradingStrategy;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TradingFlowService {

    private static final Logger log = LoggerFactory.getLogger(TradingFlowService.class);

    private final MarketPriceProvider marketPriceProvider;
    private final TradingStrategy tradingStrategy;
    private final OrderRequestFactory orderRequestFactory;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final PositionExitSignalService positionExitSignalService;

    public TradingFlowService(
            MarketPriceProvider marketPriceProvider,
            TradingStrategy tradingStrategy,
            OrderRequestFactory orderRequestFactory,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService,
            TradingFlowNotificationService tradingFlowNotificationService,
            PositionExitSignalService positionExitSignalService
    ) {
        this.marketPriceProvider = marketPriceProvider;
        this.tradingStrategy = tradingStrategy;
        this.orderRequestFactory = orderRequestFactory;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.positionExitSignalService = positionExitSignalService;
    }

    public TradingFlowResult run(String market) {
        MarketPrice marketPrice = marketPriceProvider.getCurrentPrice(market);
        TradingSignal signal = selectSignal(marketPrice, tradingStrategy.evaluate(marketPrice));
        Optional<OrderRequest> request = orderRequestFactory.create(signal);

        if (request.isEmpty()) {
            return save(new TradingFlowResult(
                    signal.market(),
                    marketPrice.currentPrice(),
                    signal.signalType(),
                    signal.reason(),
                    false,
                    null,
                    "No order created",
                    Instant.now()
            ));
        }

        OrderResult orderResult = orderExecutionService.execute(request.get());
        return save(new TradingFlowResult(
                signal.market(),
                marketPrice.currentPrice(),
                signal.signalType(),
                signal.reason(),
                true,
                orderResult.status(),
                orderResult.message(),
                orderResult.executedAt()
        ));
    }

    private TradingSignal selectSignal(MarketPrice marketPrice, TradingSignal strategySignal) {
        if (strategySignal == null || strategySignal.signalType() != SignalType.HOLD) {
            return strategySignal;
        }

        try {
            return positionExitSignalService.evaluate(marketPrice).orElse(strategySignal);
        } catch (RuntimeException exception) {
            log.warn("Position exit signal evaluation failed. market={}", marketPrice == null ? null : marketPrice.market(), exception);
            return strategySignal;
        }
    }

    private TradingFlowResult save(TradingFlowResult result) {
        tradingFlowHistoryService.save(result);
        notifyIfEnabled(result);
        return result;
    }

    private void notifyIfEnabled(TradingFlowResult result) {
        if (!notificationProperties.isEnabled() || !notificationPolicyService.shouldNotify(result)) {
            return;
        }

        try {
            tradingFlowNotificationService.notify(result);
        } catch (RuntimeException exception) {
            log.warn("Trading flow notification failed. market={}", result.market(), exception);
        }
    }
}
