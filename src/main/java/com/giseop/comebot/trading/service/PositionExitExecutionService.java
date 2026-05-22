package com.giseop.comebot.trading.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.service.PositionExitSignalService;
import com.giseop.comebot.safety.KillSwitchService;
import com.giseop.comebot.scheduler.PositionExitRunSummary;
import com.giseop.comebot.scheduler.PositionExitSchedulerProperties;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.service.OrderRequestFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PositionExitExecutionService {

    private static final Logger log = LoggerFactory.getLogger(PositionExitExecutionService.class);

    private final PaperPortfolioService paperPortfolioService;
    private final MarketPriceProvider marketPriceProvider;
    private final PositionExitSignalService positionExitSignalService;
    private final OrderRequestFactory orderRequestFactory;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final KillSwitchService killSwitchService;

    public PositionExitExecutionService(
            PaperPortfolioService paperPortfolioService,
            MarketPriceProvider marketPriceProvider,
            PositionExitSignalService positionExitSignalService,
            OrderRequestFactory orderRequestFactory,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService,
            TradingFlowNotificationService tradingFlowNotificationService,
            KillSwitchService killSwitchService
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.marketPriceProvider = marketPriceProvider;
        this.positionExitSignalService = positionExitSignalService;
        this.orderRequestFactory = orderRequestFactory;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.killSwitchService = killSwitchService;
    }

    public PositionExitRunSummary execute(ExchangeMode exchange) {
        List<String> markets = positionMarkets(exchange);
        if (markets.isEmpty()) {
            return PositionExitRunSummary.empty();
        }
        if (killSwitchService.isEnabled()) {
            return markets.stream()
                    .map(market -> save(exchange, rejected(market, "Kill switch enabled", "Kill switch enabled: position exit blocked")))
                    .map(this::summarize)
                    .reduce(new PositionExitRunSummary(markets.size(), 0, 0, 0, 0, 0), PositionExitRunSummary::add);
        }

        Map<String, MarketPrice> prices = currentPrices(markets);
        PositionExitRunSummary summary = new PositionExitRunSummary(markets.size(), 0, 0, 0, 0, 0);
        for (String market : markets) {
            MarketPrice marketPrice = prices.get(market);
            if (marketPrice == null) {
                summary = summary.add(summarize(save(exchange, failed(market, null, "Current price is not available"))));
                continue;
            }
            summary = summary.add(evaluateOne(exchange, marketPrice));
        }
        return summary;
    }

    private PositionExitRunSummary evaluateOne(ExchangeMode exchange, MarketPrice marketPrice) {
        paperPortfolioService.updatePeakPriceIfHigher(exchange, marketPrice.market(), marketPrice.currentPrice());
        try {
            Optional<TradingSignal> signal = positionExitSignalService.evaluate(exchange, marketPrice);
            if (signal.isEmpty()) {
                TradingFlowResult result = hold(marketPrice);
                if (positionExitSchedulerProperties.isSaveHoldHistory()) {
                    save(exchange, result);
                }
                return summarize(result);
            }

            Optional<OrderRequest> request = orderRequestFactory.create(signal.get());
            if (request.isEmpty()) {
                return summarize(save(exchange, failed(marketPrice.market(), marketPrice.currentPrice(), "No order created")));
            }

            OrderResult orderResult = orderExecutionService.execute(exchange, request.get());
            return summarize(save(exchange, new TradingFlowResult(
                    signal.get().market(),
                    marketPrice.currentPrice(),
                    SignalType.SELL,
                    signal.get().reason(),
                    true,
                    orderResult.status(),
                    orderResult.message(),
                    orderResult.executedAt()
            )));
        } catch (RuntimeException exception) {
            return summarize(save(exchange, failed(marketPrice.market(), marketPrice.currentPrice(), failureMessage(exception))));
        }
    }

    private Map<String, MarketPrice> currentPrices(List<String> markets) {
        try {
            Map<String, MarketPrice> prices = new LinkedHashMap<>();
            marketPriceProvider.getCurrentPrices(markets).stream()
                    .filter(price -> price != null && price.market() != null)
                    .forEach(price -> prices.put(price.market(), price));
            return prices;
        } catch (RuntimeException exception) {
            log.warn("Position exit price batch fetch failed. markets={}, error={}", markets.size(), exception.getClass().getSimpleName());
            return Map.of();
        }
    }

    private List<String> positionMarkets(ExchangeMode exchange) {
        return paperPortfolioService.findPositions(exchange).stream()
                .filter(position -> position.quantity() != null && position.quantity().signum() > 0)
                .map(PaperPosition::market)
                .filter(market -> market != null && !market.isBlank())
                .distinct()
                .toList();
    }

    private TradingFlowResult hold(MarketPrice marketPrice) {
        return new TradingFlowResult(
                marketPrice.market(),
                marketPrice.currentPrice(),
                SignalType.HOLD,
                "Position exit condition not reached",
                false,
                null,
                "Position exit skipped",
                Instant.now()
        );
    }

    private TradingFlowResult rejected(String market, String reason, String message) {
        return new TradingFlowResult(
                market,
                null,
                SignalType.SELL,
                reason,
                false,
                OrderStatus.REJECTED,
                message,
                Instant.now()
        );
    }

    private TradingFlowResult failed(String market, java.math.BigDecimal currentPrice, String message) {
        return new TradingFlowResult(
                market,
                currentPrice,
                SignalType.SELL,
                "Position exit failed",
                false,
                OrderStatus.FAILED,
                message,
                Instant.now()
        );
    }

    private TradingFlowResult save(ExchangeMode exchange, TradingFlowResult result) {
        tradingFlowHistoryService.save(exchange, result);
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
            log.warn("Position exit notification failed. market={}", result.market(), exception);
        }
    }

    private PositionExitRunSummary summarize(TradingFlowResult result) {
        if (result == null) {
            return new PositionExitRunSummary(0, 1, 0, 0, 0, 1);
        }
        if (result.orderStatus() == OrderStatus.FILLED) {
            return new PositionExitRunSummary(0, 1, 1, 0, 0, 0);
        }
        if (result.orderStatus() == OrderStatus.REJECTED) {
            return new PositionExitRunSummary(0, 1, 0, 1, 0, 0);
        }
        if (result.signalType() == SignalType.HOLD) {
            return new PositionExitRunSummary(0, 1, 0, 0, 1, 0);
        }
        return new PositionExitRunSummary(0, 1, 0, 0, 0, 1);
    }

    private String failureMessage(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
