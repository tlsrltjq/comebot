package com.giseop.comebot.history.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTradingFlowHistoryJpaRepository extends JpaRepository<TradingFlowHistoryEntity, String> {

    List<TradingFlowHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<TradingFlowHistoryEntity> findByExchangeOrderByCreatedAtDesc(ExchangeMode exchange, Pageable pageable);

    List<TradingFlowHistoryEntity> findByMarketOrderByCreatedAtDesc(String market, Pageable pageable);

    List<TradingFlowHistoryEntity> findByExchangeAndMarketOrderByCreatedAtDesc(ExchangeMode exchange, String market, Pageable pageable);

    List<TradingFlowHistoryEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant from);

    List<TradingFlowHistoryEntity> findByExchangeAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(ExchangeMode exchange, Instant from);
}
