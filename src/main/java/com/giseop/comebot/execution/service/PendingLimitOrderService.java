package com.giseop.comebot.execution.service;

import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.domain.PendingLimitOrder;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.trading.service.TradingFlowResult;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Maker-style limit entry for PAPER trading.
 *
 * <p>Lifecycle: a BUY signal places a limit order at the signal candle's close price.
 * The order fills only when a <em>fresh</em> ticker snapshot whose price is at or below
 * the limit arrives <em>after</em> the order was created. It expires unfilled after
 * {@link #EXPIRY}.
 *
 * <p>Two invariants guarantee backtest parity and safety:
 * <ol>
 *   <li><b>No same-candle fill.</b> The signal is computed from closed candles, so
 *       {@code createdAt} is strictly after the signal candle's close. We only fill on a
 *       snapshot with {@code capturedAt > createdAt}, i.e. a price observed in candle i+1
 *       or later — never the signal candle itself.</li>
 *   <li><b>No stale-price fill.</b> We require {@link TickerSnapshotStore#findFresh} within
 *       the websocket order-stale window, so a disconnected/old feed cannot trigger a fill.</li>
 * </ol>
 */
@Service
public class PendingLimitOrderService {

    private static final Logger log = LoggerFactory.getLogger(PendingLimitOrderService.class);
    private static final Duration EXPIRY = Duration.ofMinutes(5);
    private static final Duration DEFAULT_STALE = Duration.ofSeconds(30);

    private final ConcurrentHashMap<String, PendingLimitOrder> pending = new ConcurrentHashMap<>();

    private final AtomicLong totalSignals = new AtomicLong(0);
    private final AtomicLong totalFills   = new AtomicLong(0);
    private final AtomicLong totalExpired = new AtomicLong(0);
    private final AtomicLong totalSameCandleSkips = new AtomicLong(0);
    private final AtomicLong totalStaleSkips = new AtomicLong(0);

    private final TickerSnapshotStore tickerSnapshotStore;
    private final MarketWebSocketProperties marketWebSocketProperties;
    private final OrderExecutionService orderExecutionService;
    private final TradingFlowHistoryService tradingFlowHistoryService;
    private final TradingFlowNotificationService tradingFlowNotificationService;
    private final NotificationProperties notificationProperties;
    private final NotificationPolicyService notificationPolicyService;

    public PendingLimitOrderService(
            TickerSnapshotStore tickerSnapshotStore,
            MarketWebSocketProperties marketWebSocketProperties,
            OrderExecutionService orderExecutionService,
            TradingFlowHistoryService tradingFlowHistoryService,
            TradingFlowNotificationService tradingFlowNotificationService,
            NotificationProperties notificationProperties,
            NotificationPolicyService notificationPolicyService
    ) {
        this.tickerSnapshotStore = tickerSnapshotStore;
        this.marketWebSocketProperties = marketWebSocketProperties;
        this.orderExecutionService = orderExecutionService;
        this.tradingFlowHistoryService = tradingFlowHistoryService;
        this.tradingFlowNotificationService = tradingFlowNotificationService;
        this.notificationProperties = notificationProperties;
        this.notificationPolicyService = notificationPolicyService;
    }

    public void place(ExchangeMode exchange, String market, java.math.BigDecimal limitPrice,
                      java.math.BigDecimal quantity, String reason) {
        tryPlace(exchange, market, limitPrice, quantity, reason);
    }

    public boolean tryPlace(ExchangeMode exchange, String market, java.math.BigDecimal limitPrice,
                            java.math.BigDecimal quantity, String reason) {
        Instant now = Instant.now();
        PendingLimitOrder order = new PendingLimitOrder(exchange, market, limitPrice, quantity, reason, now, now.plus(EXPIRY));
        PendingLimitOrder previous = pending.putIfAbsent(key(exchange, market), order);
        if (previous != null) {
            log.info("[LIMIT-ENTRY] SKIPPED_DUPLICATE market={} limitPrice={} existingLimitPrice={}",
                    market, limitPrice, previous.limitPrice());
            return false;
        }
        totalSignals.incrementAndGet();
        log.info("[LIMIT-ENTRY] PLACED market={} limitPrice={} expiresInSec={}",
                market, limitPrice, EXPIRY.toSeconds());
        return true;
    }

    public boolean hasPending(ExchangeMode exchange, String market) {
        return pending.containsKey(key(exchange, market));
    }

    public void checkAndFillAll(ExchangeMode exchange) {
        Instant now = Instant.now();
        Duration staleAfter = staleAfter();
        for (var entry : pending.entrySet()) {
            PendingLimitOrder order = entry.getValue();
            if (order.exchange() != exchange) {
                continue;
            }
            if (now.isAfter(order.expiresAt())) {
                pending.remove(entry.getKey());
                totalExpired.incrementAndGet();
                log.info("[LIMIT-ENTRY] EXPIRED market={} limitPrice={} {}",
                        order.market(), order.limitPrice(), stats());
                continue;
            }
            Optional<TickerSnapshot> fresh = tickerSnapshotStore.findFresh(exchange, order.market(), staleAfter, now);
            if (fresh.isEmpty()) {
                // No fresh snapshot — stale feed or no data. Do not fill (safe).
                totalStaleSkips.incrementAndGet();
                continue;
            }
            TickerSnapshot snap = fresh.get();
            // Same-candle guard: only accept a price observed strictly after the order was created.
            if (!snap.capturedAt().isAfter(order.createdAt())) {
                totalSameCandleSkips.incrementAndGet();
                continue;
            }
            if (snap.tradePrice().compareTo(order.limitPrice()) <= 0) {
                pending.remove(entry.getKey());
                fill(exchange, order, snap.capturedAt());
            }
        }
    }

    private void fill(ExchangeMode exchange, PendingLimitOrder order, Instant filledAtPriceTime) {
        OrderResult result = orderExecutionService.fillLimitOrder(exchange, order);
        totalFills.incrementAndGet();
        log.info("[LIMIT-ENTRY] FILLED market={} limitPrice={} priceTime={} status={} {}",
                order.market(), order.limitPrice(), filledAtPriceTime, result.status(), stats());
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

    private Duration staleAfter() {
        if (marketWebSocketProperties == null) {
            return DEFAULT_STALE;
        }
        Duration d = marketWebSocketProperties.orderStaleDuration();
        return d == null ? DEFAULT_STALE : d;
    }

    private String stats() {
        long settled = totalFills.get() + totalExpired.get();
        double fillRate = settled == 0 ? 0.0 : totalFills.get() * 100.0 / settled;
        return String.format(
                "signals=%d fills=%d expired=%d fillRate=%.1f%% sameCandleSkips=%d staleSkips=%d",
                totalSignals.get(), totalFills.get(), totalExpired.get(), fillRate,
                totalSameCandleSkips.get(), totalStaleSkips.get());
    }

    private static String key(ExchangeMode exchange, String market) {
        return exchange.name() + ":" + market;
    }
}
