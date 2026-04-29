package com.giseop.comebot.portfolio.service;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.market.provider.MarketPriceProvider;
import com.giseop.comebot.portfolio.domain.PaperPortfolio;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.dto.PortfolioValuationResponse;
import com.giseop.comebot.portfolio.dto.PositionValuationResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaperPortfolioValuationService {

    private static final int RATE_SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaperPortfolioService paperPortfolioService;
    private final MarketPriceProvider marketPriceProvider;

    public PaperPortfolioValuationService(
            PaperPortfolioService paperPortfolioService,
            MarketPriceProvider marketPriceProvider
    ) {
        this.paperPortfolioService = paperPortfolioService;
        this.marketPriceProvider = marketPriceProvider;
    }

    public PortfolioValuationResponse valuate() {
        PaperPortfolio portfolio = paperPortfolioService.getPortfolio();
        List<PositionValuationResponse> positionValuations = new ArrayList<>();
        BigDecimal totalPositionValue = BigDecimal.ZERO;
        BigDecimal totalUnrealizedProfit = BigDecimal.ZERO;

        for (PaperPosition position : portfolio.positions()) {
            MarketPrice currentPrice = marketPriceProvider.getCurrentPrice(position.market());
            if (currentPrice == null || currentPrice.currentPrice() == null) {
                throw new IllegalStateException("Current price is not available");
            }

            PositionValuationResponse valuation = valuatePosition(position, currentPrice.currentPrice());
            positionValuations.add(valuation);
            totalPositionValue = totalPositionValue.add(valuation.positionValue());
            totalUnrealizedProfit = totalUnrealizedProfit.add(valuation.unrealizedProfit());
        }

        BigDecimal totalEquity = portfolio.cash().add(totalPositionValue);
        BigDecimal totalProfit = portfolio.realizedProfit().add(totalUnrealizedProfit);
        return new PortfolioValuationResponse(
                portfolio.cash(),
                totalPositionValue,
                totalEquity,
                portfolio.realizedProfit(),
                totalUnrealizedProfit,
                totalProfit,
                positionValuations
        );
    }

    private PositionValuationResponse valuatePosition(PaperPosition position, BigDecimal currentPrice) {
        BigDecimal positionValue = position.quantity().multiply(currentPrice);
        BigDecimal cost = position.quantity().multiply(position.averageBuyPrice());
        BigDecimal unrealizedProfit = positionValue.subtract(cost);
        BigDecimal unrealizedProfitRate = BigDecimal.ZERO;
        if (cost.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedProfitRate = unrealizedProfit
                    .divide(cost, RATE_SCALE, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
        }

        return new PositionValuationResponse(
                position.market(),
                position.quantity(),
                position.averageBuyPrice(),
                currentPrice,
                positionValue,
                unrealizedProfit,
                unrealizedProfitRate
        );
    }
}
