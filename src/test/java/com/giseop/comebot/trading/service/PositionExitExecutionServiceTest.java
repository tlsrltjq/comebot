package com.giseop.comebot.trading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderResult;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.execution.domain.OrderStatus;
import com.giseop.comebot.execution.service.OrderExecutionService;
import com.giseop.comebot.history.repository.InMemoryTradingFlowHistoryRepository;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PositionExitExecutionServiceTest {

    private final PaperPortfolioService paperPortfolioService = mock(PaperPortfolioService.class);
    private final MarketPriceProvider marketPriceProvider = mock(MarketPriceProvider.class);
    private final PositionExitSignalService positionExitSignalService = mock(PositionExitSignalService.class);
    private final OrderExecutionService orderExecutionService = mock(OrderExecutionService.class);
    private final KillSwitchService killSwitchService = mock(KillSwitchService.class);
    private final InMemoryTradingFlowHistoryRepository historyRepository = new InMemoryTradingFlowHistoryRepository();
    private final PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();

    @Test
    void executeDoesNothingWhenNoPositionsExist() {
        when(paperPortfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of());

        PositionExitRunSummary summary = service().execute(ExchangeMode.UPBIT);

        assertThat(summary).isEqualTo(PositionExitRunSummary.empty());
        verify(marketPriceProvider, never()).getCurrentPrices(any());
    }

    @Test
    void executeSellsWhenExitSignalIsCreated() {
        when(paperPortfolioService.findPositions(ExchangeMode.UPBIT))
                .thenReturn(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.1"), new BigDecimal("100"))));
        MarketPrice price = new MarketPrice("KRW-BTC", new BigDecimal("102"), Instant.now());
        when(marketPriceProvider.getCurrentPrices(List.of("KRW-BTC"))).thenReturn(List.of(price));
        when(positionExitSignalService.evaluate(eq(ExchangeMode.UPBIT), eq(price)))
                .thenReturn(Optional.of(new TradingSignal(
                        "KRW-BTC",
                        SignalType.SELL,
                        "Take profit rate reached: 2.00000000",
                        new BigDecimal("102"),
                        new BigDecimal("0.1"),
                        Instant.now()
                )));
        when(orderExecutionService.execute(eq(ExchangeMode.UPBIT), any(OrderRequest.class)))
                .thenReturn(new OrderResult(
                        "KRW-BTC",
                        OrderSide.SELL,
                        new BigDecimal("0.1"),
                        new BigDecimal("102"),
                        OrderStatus.FILLED,
                        "Paper order filled",
                        Instant.now()
                ));

        PositionExitRunSummary summary = service().execute(ExchangeMode.UPBIT);

        assertThat(summary.soldCount()).isEqualTo(1);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10)).hasSize(1);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().signalType()).isEqualTo(SignalType.SELL);
    }

    @Test
    void executeDoesNotSaveHoldHistoryByDefault() {
        when(paperPortfolioService.findPositions(ExchangeMode.UPBIT))
                .thenReturn(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.1"), new BigDecimal("100"))));
        MarketPrice price = new MarketPrice("KRW-BTC", new BigDecimal("100.5"), Instant.now());
        when(marketPriceProvider.getCurrentPrices(List.of("KRW-BTC"))).thenReturn(List.of(price));
        when(positionExitSignalService.evaluate(eq(ExchangeMode.UPBIT), eq(price))).thenReturn(Optional.empty());

        PositionExitRunSummary summary = service().execute(ExchangeMode.UPBIT);

        assertThat(summary.holdCount()).isEqualTo(1);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10)).isEmpty();
    }

    @Test
    void executeSavesRejectedHistoryWhenKillSwitchIsEnabled() {
        when(paperPortfolioService.findPositions(ExchangeMode.UPBIT))
                .thenReturn(List.of(new PaperPosition("KRW-BTC", new BigDecimal("0.1"), new BigDecimal("100"))));
        when(killSwitchService.isEnabled()).thenReturn(true);

        PositionExitRunSummary summary = service().execute(ExchangeMode.UPBIT);

        assertThat(summary.rejectedCount()).isEqualTo(1);
        assertThat(historyRepository.findRecent(ExchangeMode.UPBIT, 10).getFirst().orderStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    private PositionExitExecutionService service() {
        return new PositionExitExecutionService(
                paperPortfolioService,
                marketPriceProvider,
                positionExitSignalService,
                new OrderRequestFactory(),
                orderExecutionService,
                new TradingFlowHistoryService(historyRepository),
                properties,
                new NotificationProperties(),
                mock(NotificationPolicyService.class),
                mock(TradingFlowNotificationService.class),
                killSwitchService
        );
    }
}
