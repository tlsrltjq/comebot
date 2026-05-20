package com.giseop.comebot.scanlog.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.scanlog.repository.CandidateScanLogRepository;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CandidateScanLogService {

    private final CandidateScanLogRepository repository;

    public CandidateScanLogService(CandidateScanLogRepository repository) {
        this.repository = repository;
    }

    public CandidateScanLog save(ExchangeMode exchange, TradingCandidate candidate) {
        CandidateScanLog log = new CandidateScanLog(
                UUID.randomUUID().toString(),
                exchange == null ? ExchangeMode.UPBIT : exchange,
                candidate.market(),
                candidate.decision(),
                candidate.reason(),
                candidate.currentPrice(),
                candidate.priceChangeRate(),
                candidate.highLowRangeRate(),
                candidate.tradeAmountChangeRate(),
                candidate.trend(),
                candidate.lastCandleBullish(),
                candidate.scannedAt() != null ? candidate.scannedAt() : Instant.now()
        );
        return repository.save(log);
    }

    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from) {
        return repository.findSince(exchange, from);
    }

    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from, CandidateDecision decision) {
        return repository.findSince(exchange, from, decision);
    }
}
