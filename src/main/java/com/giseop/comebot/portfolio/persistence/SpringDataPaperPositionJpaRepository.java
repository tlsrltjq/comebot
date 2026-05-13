package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaperPositionJpaRepository extends JpaRepository<PaperPositionEntity, PaperPositionId> {

    Optional<PaperPositionEntity> findByExchangeAndMarket(ExchangeMode exchange, String market);

    List<PaperPositionEntity> findByExchangeAndQuantityGreaterThanOrderByMarketAsc(ExchangeMode exchange, BigDecimal quantity);
}
