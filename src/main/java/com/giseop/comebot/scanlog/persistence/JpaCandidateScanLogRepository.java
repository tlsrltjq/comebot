package com.giseop.comebot.scanlog.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.scanlog.domain.CandidateScanLog;
import com.giseop.comebot.scanlog.repository.CandidateScanLogRepository;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "candidate.scan-log.storage-type", havingValue = "JPA")
public class JpaCandidateScanLogRepository implements CandidateScanLogRepository {

    private final SpringDataCandidateScanLogJpaRepository jpaRepository;

    public JpaCandidateScanLogRepository(SpringDataCandidateScanLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CandidateScanLog save(CandidateScanLog log) {
        return jpaRepository.save(CandidateScanLogEntity.from(log)).toDomain();
    }

    @Override
    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from) {
        return jpaRepository
                .findByExchangeAndScannedAtGreaterThanEqualOrderByScannedAtDesc(exchange, from)
                .stream()
                .map(CandidateScanLogEntity::toDomain)
                .toList();
    }

    @Override
    public List<CandidateScanLog> findSince(ExchangeMode exchange, Instant from, CandidateDecision decision) {
        if (decision == null) {
            return findSince(exchange, from);
        }
        return jpaRepository
                .findByExchangeAndDecisionAndScannedAtGreaterThanEqualOrderByScannedAtDesc(exchange, decision, from)
                .stream()
                .map(CandidateScanLogEntity::toDomain)
                .toList();
    }
}
