package com.giseop.comebot.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.domain.PendingLimitOrder;
import com.giseop.comebot.history.service.TradingFlowHistoryService;
import com.giseop.comebot.market.domain.PriceSource;
import com.giseop.comebot.market.domain.TickerSnapshot;
import com.giseop.comebot.market.service.TickerSnapshotStore;
import com.giseop.comebot.market.websocket.MarketWebSocketProperties;
import com.giseop.comebot.notification.NotificationPolicyService;
import com.giseop.comebot.notification.NotificationProperties;
import com.giseop.comebot.notification.TradingFlowNotificationService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingLimitOrderServiceTest {

    @Mock
    private TickerSnapshotStore tickerSnapshotStore;
    @Mock
    private OrderExecutionService orderExecutionService;
    @Mock
    private TradingFlowHistoryService tradingFlowHistoryService;
    @Mock
    private TradingFlowNotificationService tradingFlowNotificationService;
    @Mock
    private NotificationPolicyService notificationPolicyService;

    private final NotificationProperties notificationProperties = new NotificationProperties();
    private final MarketWebSocketProperties webSocketProperties = new MarketWebSocketProperties();
    private PendingLimitOrderService service;

    @BeforeEach
    void setUp() {
        service = new PendingLimitOrderService(
                tickerSnapshotStore,
                webSocketProperties,
                orderExecutionService,
                tradingFlowHistoryService,
                tradingFlowNotificationService,
                notificationProperties,
                notificationPolicyService
        );
    }

    private TickerSnapshot snapshot(BigDecimal price, Instant capturedAt) {
        return new TickerSnapshot(ExchangeMode.UPBIT, "KRW-BTC", price,
                new BigDecimal("1000000"), capturedAt, PriceSource.WEBSOCKET);
    }

    private OrderResult filledResult() {
        return new OrderResult("KRW-BTC", OrderSide.BUY, new BigDecimal("1"),
                new BigDecimal("100"), OrderStatus.FILLED, "filled", Instant.now());
    }

    @Test
    void doesNotFillWhenSnapshotCapturedBeforeOrderCreated() {
        // same-candle guard: a price observed at/before createdAt (signal candle) must not fill
        service.place(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"), new BigDecimal("1"), "reason");
        // snapshot captured in the past (before createdAt), price below limit
        Instant past = Instant.now().minusSeconds(60);
        when(tickerSnapshotStore.findFresh(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(Duration.class), any(Instant.class)))
                .thenReturn(Optional.of(snapshot(new BigDecimal("90"), past)));

        service.checkAndFillAll(ExchangeMode.UPBIT);

        verify(orderExecutionService, never()).fillLimitOrder(any(), any());
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).isTrue();
    }

    @Test
    void doesNotFillWhenNoFreshSnapshot() {
        // stale-price guard: empty fresh snapshot means stale/missing feed → no fill
        service.place(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"), new BigDecimal("1"), "reason");
        when(tickerSnapshotStore.findFresh(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(Duration.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        service.checkAndFillAll(ExchangeMode.UPBIT);

        verify(orderExecutionService, never()).fillLimitOrder(any(), any());
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).isTrue();
    }

    @Test
    void doesNotFillWhenPriceAboveLimit() {
        service.place(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"), new BigDecimal("1"), "reason");
        Instant future = Instant.now().plusSeconds(1);
        when(tickerSnapshotStore.findFresh(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(Duration.class), any(Instant.class)))
                .thenReturn(Optional.of(snapshot(new BigDecimal("105"), future)));

        service.checkAndFillAll(ExchangeMode.UPBIT);

        verify(orderExecutionService, never()).fillLimitOrder(any(), any());
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).isTrue();
    }

    @Test
    void fillsWhenFreshSnapshotAfterCreatedAndPriceAtOrBelowLimit() {
        when(orderExecutionService.fillLimitOrder(eq(ExchangeMode.UPBIT), any(PendingLimitOrder.class)))
                .thenReturn(filledResult());
        service.place(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"), new BigDecimal("1"), "reason");
        Instant after = Instant.now().plusSeconds(2);
        when(tickerSnapshotStore.findFresh(eq(ExchangeMode.UPBIT), eq("KRW-BTC"), any(Duration.class), any(Instant.class)))
                .thenReturn(Optional.of(snapshot(new BigDecimal("99"), after)));

        service.checkAndFillAll(ExchangeMode.UPBIT);

        verify(orderExecutionService).fillLimitOrder(eq(ExchangeMode.UPBIT), any(PendingLimitOrder.class));
        verify(tradingFlowHistoryService).save(eq(ExchangeMode.UPBIT), any());
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).isFalse();
    }

    @Test
    void duplicatePendingPreventedByHasPending() {
        boolean firstPlaced = service.tryPlace(
                ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"), new BigDecimal("1"), "reason");
        boolean duplicatePlaced = service.tryPlace(
                ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("90"), new BigDecimal("1"), "reason");

        assertThat(firstPlaced).isTrue();
        assertThat(duplicatePlaced).isFalse();
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-BTC")).isTrue();
        assertThat(service.hasPending(ExchangeMode.UPBIT, "KRW-ETH")).isFalse();
    }

    @Test
    void otherExchangeOrdersAreNotTouched() {
        service.place(ExchangeMode.BINANCE, "BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), "reason");
        // checking UPBIT must not fill or expire the BINANCE order
        service.checkAndFillAll(ExchangeMode.UPBIT);
        assertThat(service.hasPending(ExchangeMode.BINANCE, "BTCUSDT")).isTrue();
        verify(orderExecutionService, never()).fillLimitOrder(any(), any());
    }
}
