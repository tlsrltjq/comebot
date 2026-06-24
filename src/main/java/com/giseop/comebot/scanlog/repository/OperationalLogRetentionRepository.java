package com.giseop.comebot.scanlog.repository;

import java.time.Instant;

public interface OperationalLogRetentionRepository {

    int summarizeCandidateScansBefore(Instant cutoff);

    int deleteCandidateScansBefore(Instant cutoff);

    int deleteTradingFlowHistoryBefore(Instant cutoff);
}
