package com.giseop.comebot.scheduler;

import com.giseop.comebot.trading.service.PositionExitExecutionService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPositionExitRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPositionExitRunner.class);

    private final PositionExitSchedulerProperties positionExitSchedulerProperties;
    private final PositionExitExecutionService positionExitExecutionService;
    private final AtomicBoolean running;

    @Autowired
    public ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService
    ) {
        this(positionExitSchedulerProperties, positionExitExecutionService, new AtomicBoolean(false));
    }

    ScheduledPositionExitRunner(
            PositionExitSchedulerProperties positionExitSchedulerProperties,
            PositionExitExecutionService positionExitExecutionService,
            AtomicBoolean running
    ) {
        this.positionExitSchedulerProperties = positionExitSchedulerProperties;
        this.positionExitExecutionService = positionExitExecutionService;
        this.running = running;
    }

    @Scheduled(fixedDelayString = "${trading.exit-scheduler.fixed-delay-ms:5000}")
    public void runScheduled() {
        runOnce();
    }

    public PositionExitRunSummary runOnce() {
        if (!positionExitSchedulerProperties.isEnabled()) {
            return PositionExitRunSummary.empty();
        }
        if (!running.compareAndSet(false, true)) {
            log.info("Scheduled position exit skipped because previous run is still active");
            return PositionExitRunSummary.empty();
        }
        try {
            PositionExitRunSummary summary = PositionExitRunSummary.empty();
            for (var exchange : positionExitSchedulerProperties.getExchanges()) {
                summary = summary.add(positionExitExecutionService.execute(exchange));
            }
            if (summary.positionMarkets() > 0) {
                log.info(
                        "Scheduled position exit summary. positions={}, evaluated={}, sold={}, rejected={}, hold={}, failed={}",
                        summary.positionMarkets(),
                        summary.evaluatedMarkets(),
                        summary.soldCount(),
                        summary.rejectedCount(),
                        summary.holdCount(),
                        summary.failedCount()
                );
            }
            return summary;
        } catch (RuntimeException exception) {
            log.warn("Scheduled position exit failed. error={}", exception.getClass().getSimpleName());
            return new PositionExitRunSummary(0, 1, 0, 0, 0, 1);
        } finally {
            running.set(false);
        }
    }
}
