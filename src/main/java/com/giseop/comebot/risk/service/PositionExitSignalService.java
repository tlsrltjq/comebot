package com.giseop.comebot.risk.service;

import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionExitProperties;
import com.giseop.comebot.risk.domain.PositionExitPolicy;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PositionExitSignalService {

    private static final int RATE_SCALE = 8;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PositionExitProperties positionExitProperties;
    private final PaperPortfolioService paperPortfolioService;

    public PositionExitSignalService(
            PositionExitProperties positionExitProperties,
            PaperPortfolioService paperPortfolioService
    ) {
        this.positionExitProperties = positionExitProperties;
        this.paperPortfolioService = paperPortfolioService;
    }

    public Optional<TradingSignal> evaluate(MarketPrice marketPrice) {
        return evaluate(paperPortfolioService.findPositions(), marketPrice);
    }

    public Optional<TradingSignal> evaluate(ExchangeMode exchange, MarketPrice marketPrice) {
        return evaluate(paperPortfolioService.findPositions(exchange), marketPrice);
    }

    private Optional<TradingSignal> evaluate(java.util.List<PaperPosition> positions, MarketPrice marketPrice) {
        PositionExitPolicy policy = currentPolicy();
        if (!policy.enabled() || marketPrice == null || marketPrice.market() == null || marketPrice.currentPrice() == null) {
            return Optional.empty();
        }

        java.util.List<PaperPosition> safePositions = positions == null ? java.util.List.of() : positions;
        Optional<PaperPosition> position = safePositions.stream()
                .filter(candidate -> marketPrice.market().equals(candidate.market()))
                .filter(candidate -> candidate.quantity() != null && candidate.quantity().compareTo(BigDecimal.ZERO) > 0)
                .filter(candidate -> candidate.averageBuyPrice() != null && candidate.averageBuyPrice().compareTo(BigDecimal.ZERO) > 0)
                .findFirst();

        if (position.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal profitRate = profitRate(position.get(), marketPrice.currentPrice());
        if (profitRate.compareTo(policy.takeProfitRate()) >= 0) {
            return Optional.of(sellSignal(marketPrice, position.get(), "Take profit rate reached: " + profitRate));
        }
        if (profitRate.compareTo(policy.stopLossRate()) <= 0) {
            return Optional.of(sellSignal(marketPrice, position.get(), "Stop loss rate reached: " + profitRate));
        }
        return Optional.empty();
    }

    public PositionExitPolicy currentPolicy() {
        return new PositionExitPolicy(
                positionExitProperties.isPositionExitEnabled(),
                positionExitProperties.getTakeProfitRate(),
                positionExitProperties.getStopLossRate()
        );
    }

    private TradingSignal sellSignal(MarketPrice marketPrice, PaperPosition position, String reason) {
        return new TradingSignal(
                marketPrice.market(),
                SignalType.SELL,
                reason,
                marketPrice.currentPrice(),
                position.quantity(),
                Instant.now()
        );
    }

    private BigDecimal profitRate(PaperPosition position, BigDecimal currentPrice) {
        return currentPrice.subtract(position.averageBuyPrice())
                .divide(position.averageBuyPrice(), RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);
    }
}
