package com.giseop.comebot.scanlog.repository;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "candidate.scan-log.storage-type", havingValue = "IN_MEMORY", matchIfMissing = true)
public class InMemoryCandidateScanLogRepository implements CandidateScanLogRepository {

    private static final int MAX_SIZE = 10_000;

    private final Deque<CandidateScanLog> store = new ConcurrentLinkedDeque<>();

    @Override
    public CandidateScanLog save(CandidateScanLog log) {
        store.addFirst(log);
        while (store.size() > MAX_SIZE) {
            store.pollLast();
        }
        return log;
    }

    @Override
    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from) {
        return store.stream()
                .filter(log -> exchange == null || exchange == log.exchange())
                .filter(log -> !log.scannedAt().isBefore(from))
                .toList();
    }

    @Override
    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from, CandidateDecision decision) {
        return store.stream()
                .filter(log -> exchange == null || exchange == log.exchange())
                .filter(log -> !log.scannedAt().isBefore(from))
                .filter(log -> decision == null || decision == log.decision())
                .toList();
    }
}
