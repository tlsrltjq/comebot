package com.giseop.comebot.strategy.service;

import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PositionEntryGuardService {

    private final PaperPortfolioService paperPortfolioService;
    private final StrategyEntryProperties strategyEntryProperties;

    public PositionEntryGuardService(
            PaperPortfolioService paperPortfolioService,
            StrategyEntryProperties strategyEntryProperties
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.strategyEntryProperties = strategyEntryProperties;
    }

    public boolean shouldBlockEntry(String market) {
        if (!strategyEntryProperties.isPreventReentryWithPosition() || market == null || market.isBlank()) {
            return false;
        }
        return paperPortfolioService.findPositions().stream()
                .filter(position -> market.equals(position.market()))
                .map(PaperPosition::quantity)
                .anyMatch(quantity -> quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0);
    }
}
