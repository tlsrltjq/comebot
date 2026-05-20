package com.giseop.comebot.scanlog.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCandidateScanLogJpaRepository extends JpaRepository<CandidateScanLogEntity, String> {

    List<CandidateScanLogEntity> findByExchangeAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
            ExchangeMode exchange, Instant from);

    List<CandidateScanLogEntity> findByExchangeAndDecisionAndScannedAtGreaterThanEqualOrderByScannedAtDesc(
            ExchangeMode exchange, CandidateDecision decision, Instant from);
}
