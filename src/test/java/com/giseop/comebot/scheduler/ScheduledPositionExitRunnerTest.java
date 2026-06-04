package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.service.PendingLimitOrderService;
import com.giseop.comebot.market.service.MarketDataReadiness;
import com.giseop.comebot.market.service.MarketDataReadinessService;
import com.giseop.comebot.trading.service.PositionExitExecutionService;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ScheduledPositionExitRunnerTest {

    @Test
    void runOnceDoesNotExecuteWhenDisabled() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        properties.setEnabled(false);
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(properties, service).runOnce();

        assertThat(summary).isEqualTo(PositionExitRunSummary.empty());
        verify(service, never()).execute(properties.getExchange());
    }

    @Test
    void runOnceExecutesWhenEnabled() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        properties.setEnabled(true);
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);
        when(service.execute(properties.getExchange())).thenReturn(new PositionExitRunSummary(1, 1, 1, 0, 0, 0));

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(properties, service).runOnce();

        assertThat(summary.soldCount()).isEqualTo(1);
        verify(service).execute(properties.getExchange());
    }

    @Test
    void runOnceCanExecuteUpbitAndBinanceInSameRun() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        properties.setEnabled(true);
        properties.setExchanges(List.of(ExchangeMode.UPBIT, ExchangeMode.BINANCE));
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);
        when(service.execute(ExchangeMode.UPBIT)).thenReturn(new PositionExitRunSummary(1, 1, 0, 0, 1, 0));
        when(service.execute(ExchangeMode.BINANCE)).thenReturn(new PositionExitRunSummary(2, 2, 1, 0, 1, 0));

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(properties, service).runOnce();

        assertThat(summary.positionMarkets()).isEqualTo(3);
        assertThat(summary.evaluatedMarkets()).isEqualTo(3);
        assertThat(summary.soldCount()).isEqualTo(1);
        assertThat(summary.holdCount()).isEqualTo(2);
        verify(service).execute(ExchangeMode.UPBIT);
        verify(service).execute(ExchangeMode.BINANCE);
    }

    @Test
    void runOnceSkipsWhenPreviousRunIsActive() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(properties, service, new AtomicBoolean(true)).runOnce();

        assertThat(summary).isEqualTo(PositionExitRunSummary.empty());
        verify(service, never()).execute(properties.getExchange());
    }

    @Test
    void runOnceSkipsExchangeWhenMarketDataIsNotReady() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        properties.setEnabled(true);
        properties.setExchanges(List.of(ExchangeMode.UPBIT));
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);
        MarketDataReadinessService readinessService = mock(MarketDataReadinessService.class);
        when(readinessService.readiness(ExchangeMode.UPBIT))
                .thenReturn(MarketDataReadiness.snapshot(ExchangeMode.UPBIT, 0, 0));

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(
                properties,
                service,
                readinessService,
                mock(PendingLimitOrderService.class),
                new AtomicBoolean(false)
        ).runOnce();

        assertThat(summary).isEqualTo(PositionExitRunSummary.empty());
        verify(service, never()).execute(ExchangeMode.UPBIT);
    }
}
