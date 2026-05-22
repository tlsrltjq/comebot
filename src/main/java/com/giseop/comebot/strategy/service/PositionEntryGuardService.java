package com.giseop.comebot.strategy.service;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
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
        return shouldBlockEntry(ExchangeMode.UPBIT, market, null);
    }

    public boolean shouldBlockEntry(ExchangeMode exchange, String market) {
        return shouldBlockEntry(exchange, market, null);
    }

    public boolean shouldBlockEntry(ExchangeMode exchange, String market, BigDecimal currentPrice) {
        if (market == null || market.isBlank()) {
            return false;
        }
        Optional<PaperPosition> existing = paperPortfolioService.findPositions(exchange).stream()
                .filter(p -> market.equals(p.market()))
                .filter(p -> p.quantity() != null && p.quantity().compareTo(BigDecimal.ZERO) > 0)
                .findFirst();

        if (existing.isEmpty()) {
            return false;
        }
        if (strategyEntryProperties.isPreventReentryWithPosition()) {
            return true;
        }
        BigDecimal minProfitRate = strategyEntryProperties.getMinReentryProfitRate();
        if (minProfitRate.compareTo(BigDecimal.ZERO) <= 0 || currentPrice == null
                || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal avgBuyPrice = existing.get().averageBuyPrice();
        if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal unrealizedProfitRate = currentPrice.subtract(avgBuyPrice)
                .divide(avgBuyPrice, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return unrealizedProfitRate.compareTo(minProfitRate) < 0;
    }
}
