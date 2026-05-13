package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaperRealizedProfitEventJpaRepository extends JpaRepository<PaperRealizedProfitEventEntity, String> {

    List<PaperRealizedProfitEventEntity> findByExchangeAndRealizedAtGreaterThanEqualOrderByRealizedAtDesc(
            ExchangeMode exchange,
            Instant realizedAt
    );
}
