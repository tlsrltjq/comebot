package com.giseop.comebot.scanlog.repository;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.util.List;

public interface CandidateScanLogRepository {

    CandidateScanLog save(CandidateScanLog log);

    List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from);

    List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from, CandidateDecision decision);
}
