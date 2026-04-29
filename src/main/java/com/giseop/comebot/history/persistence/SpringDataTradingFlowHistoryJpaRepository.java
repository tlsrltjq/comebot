package com.giseop.comebot.history.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTradingFlowHistoryJpaRepository extends JpaRepository<TradingFlowHistoryEntity, String> {

    List<TradingFlowHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<TradingFlowHistoryEntity> findByMarketOrderByCreatedAtDesc(String market, Pageable pageable);

    List<TradingFlowHistoryEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant from);
}
