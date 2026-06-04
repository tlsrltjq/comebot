package com.giseop.comebot.execution.service;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.domain.PendingLimitOrder;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PendingLimitOrderService {

    private static final Logger log = LoggerFactory.getLogger(PendingLimitOrderService.class);
    private static final Duration EXPIRY = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, PendingLimitOrder> pending = new ConcurrentHashMap<>();

    private final TickerSnapshotStore tickerSnapshotStore;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;

    public PendingLimitOrderService(
            TickerSnapshotStore tickerSnapshotStore,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            TradingFlowNotificationService tradingFlowNotificationService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService
    ) {
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
    }

    public void place(ExchangeMode exchange, String market, BigDecimal limitPrice, BigDecimal quantity, String reason) {
        Instant now = Instant.now();
        pending.put(key(exchange, market),
                new PendingLimitOrder(exchange, market, limitPrice, quantity, reason, now, now.plus(EXPIRY)));
        log.info("Limit order placed. market={} limitPrice={}", market, limitPrice);
    }

    public boolean hasPending(ExchangeMode exchange, String market) {
        return pending.containsKey(key(exchange, market));
    }

    public void checkAndFillAll(ExchangeMode exchange) {
        Instant now = Instant.now();
        for (var entry : pending.entrySet()) {
            PendingLimitOrder order = entry.getValue();
            if (order.exchange() != exchange) {
                continue;
            }
            if (now.isAfter(order.expiresAt())) {
                pending.remove(entry.getKey());
                log.info("Limit order expired without fill. market={} limitPrice={}", order.market(), order.limitPrice());
                continue;
            }
            tickerSnapshotStore.find(exchange, order.market()).ifPresent(snap -> {
                if (snap.tradePrice().compareTo(order.limitPrice()) <= 0) {
                    pending.remove(entry.getKey());
                    fill(exchange, order);
                }
            });
        }
    }

    private void fill(ExchangeMode exchange, PendingLimitOrder order) {
        OrderResult result = orderExecutionService.fillLimitOrder(exchange, order);
        log.info("Limit order filled. market={} price={} status={}", order.market(), order.limitPrice(), result.status());
        TradingFlowResult flowResult = new TradingFlowResult(
                order.market(),
                order.limitPrice(),
                SignalType.BUY,
                order.reason(),
                result.status() == OrderStatus.FILLED,
                result.status(),
                result.message(),
                result.executedAt()
        );
        tradingFlowHistoryService.save(exchange, flowResult);
        if (notificationProperties.isEnabled() && notificationPolicyService.shouldNotify(flowResult)) {
            try {
                tradingFlowNotificationService.notify(flowResult);
            } catch (RuntimeException e) {
                log.warn("Limit fill notification failed. market={}", order.market(), e);
            }
        }
    }

    private static String key(ExchangeMode exchange, String market) {
        return exchange.name() + ":" + market;
    }
}
