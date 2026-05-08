package com.giseop.comebot.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giseop.comebot.trading.service.PositionExitExecutionService;
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
    void runOnceSkipsWhenPreviousRunIsActive() {
        PositionExitSchedulerProperties properties = new PositionExitSchedulerProperties();
        PositionExitExecutionService service = mock(PositionExitExecutionService.class);

        PositionExitRunSummary summary = new ScheduledPositionExitRunner(properties, service, new AtomicBoolean(true)).runOnce();

        assertThat(summary).isEqualTo(PositionExitRunSummary.empty());
        verify(service, never()).execute(properties.getExchange());
    }
}
