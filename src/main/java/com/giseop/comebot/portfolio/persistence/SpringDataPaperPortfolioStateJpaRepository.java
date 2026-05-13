package com.giseop.comebot.portfolio.persistence;

import com.giseop.comebot.exchange.ExchangeMode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaperPortfolioStateJpaRepository extends JpaRepository<PaperPortfolioStateEntity, ExchangeMode> {
}
