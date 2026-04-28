package com.giseop.comebot.portfolio.repository;

import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaperPortfolioRepository {

    BigDecimal getCash();

    void saveCash(BigDecimal cash);

    BigDecimal getRealizedProfit();

    void saveRealizedProfit(BigDecimal realizedProfit);

    Optional<PaperPosition> findPosition(String market);

    List<PaperPosition> findPositions();

    void savePosition(PaperPosition position);

    PaperPortfolio getPortfolio();
}
