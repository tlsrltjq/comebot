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
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PendingLimitOrderService {

    private static final Logger log = LoggerFactory.getLogger(PendingLimitOrderService.class);
    private static final Duration EXPIRY = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, PendingLimitOrder> pending = new ConcurrentHashMap<>();

    // stats counters (reset on each fill/expire cycle, used for logging)
    private final AtomicLong totalSignals = new AtomicLong(0);
    private final AtomicLong totalFills   = new AtomicLong(0);
    private final AtomicLong totalExpired = new AtomicLong(0);

    private final CandidateScannerProperties candidateScannerProperties;
    private final TickerSnapshotStore tickerSnapshotStore;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;

    public PendingLimitOrderService(
            CandidateScannerProperties candidateScannerProperties,
            TickerSnapshotStore tickerSnapshotStore,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            TradingFlowNotificationService tradingFlowNotificationService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService
    ) {
        this.candidateScannerProperties = candidateScannerProperties;
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
    }

    /**
     * Place a limit buy order at limitPrice.
     * Fill check is deferred by one candle unit to prevent same-candle fill —
     * signal fires after candle i closes; fill can only happen on candle i+1 or later.
     */
    public void place(ExchangeMode exchange, String market, BigDecimal limitPrice, BigDecimal quantity, String reason) {
        Instant now = Instant.now();
        int candleUnitMinutes = candidateScannerProperties.getCandleUnitMinutes(exchange);
        Instant firstCheckAt = now.plus(Duration.ofSeconds(candleUnitMinutes * 60L));
        Instant expiresAt    = now.plus(EXPIRY);
        pending.put(key(exchange, market),
                new PendingLimitOrder(exchange, market, limitPrice, quantity, reason,
                        now, firstCheckAt, expiresAt));
        totalSignals.incrementAndGet();
        log.info("[LIMIT-ENTRY] PLACED market={} limitPrice={} firstCheckAt={}s candleUnit={}m",
                market, limitPrice, candleUnitMinutes * 60, candleUnitMinutes);
    }

    public boolean hasPending(ExchangeMode exchange, String market) {
        return pending.containsKey(key(exchange, market));
    }

    /**
     * Called each exit-scheduler tick.
     * Skips fill check before firstCheckAt (same-candle fill guard).
     */
    public void checkAndFillAll(ExchangeMode exchange) {
        Instant now = Instant.now();
        for (var entry : pending.entrySet()) {
            PendingLimitOrder order = entry.getValue();
            if (order.exchange() != exchange) {
                continue;
            }
            if (now.isAfter(order.expiresAt())) {
                pending.remove(entry.getKey());
                totalExpired.incrementAndGet();
                log.info("[LIMIT-ENTRY] EXPIRED  market={} limitPrice={} signals={} fills={} expired={} fillRate={:.1f}%",
                        order.market(), order.limitPrice(),
                        totalSignals.get(), totalFills.get(), totalExpired.get(), fillRate());
                continue;
            }
            // Same-candle fill guard: do not check fill until one candle unit has elapsed
            if (now.isBefore(order.firstCheckAt())) {
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
        totalFills.incrementAndGet();
        log.info("[LIMIT-ENTRY] FILLED  market={} price={} status={} signals={} fills={} expired={} fillRate={:.1f}%",
                order.market(), order.limitPrice(), result.status(),
                totalSignals.get(), totalFills.get(), totalExpired.get(), fillRate());
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
                log.warn("[LIMIT-ENTRY] Notification failed. market={}", order.market(), e);
            }
        }
    }

    private double fillRate() {
        long settled = totalFills.get() + totalExpired.get();
        return settled == 0 ? 0.0 : totalFills.get() * 100.0 / settled;
    }

    private static String key(ExchangeMode exchange, String market) {
        return exchange.name() + ":" + market;
    }
}
