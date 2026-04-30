package com.giseop.comebot.scheduler;

public record CandidateSchedulerRunSummary(
        int requestedMarkets,
        int executedMarkets,
        int filledCount,
        int rejectedCount,
        int holdCount,
        int failedCount
) {

    public static CandidateSchedulerRunSummary empty() {
        return new CandidateSchedulerRunSummary(0, 0, 0, 0, 0, 0);
    }

    public CandidateSchedulerRunSummary add(CandidateSchedulerRunSummary other) {
        return new CandidateSchedulerRunSummary(
                requestedMarkets + other.requestedMarkets(),
                executedMarkets + other.executedMarkets(),
                filledCount + other.filledCount(),
                rejectedCount + other.rejectedCount(),
                holdCount + other.holdCount(),
                failedCount + other.failedCount()
        );
    }
}
