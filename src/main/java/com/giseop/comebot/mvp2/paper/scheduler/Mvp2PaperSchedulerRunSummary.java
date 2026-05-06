package com.giseop.comebot.mvp2.paper.scheduler;

import java.time.Instant;

public record Mvp2PaperSchedulerRunSummary(
        boolean enabled,
        int requestedSymbols,
        int executedSymbols,
        int buyCount,
        int sellCount,
        int holdCount,
        int failedCount,
        boolean skippedBecauseRunning,
        Instant ranAt
) {
}
