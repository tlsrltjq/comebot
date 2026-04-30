package com.giseop.comebot.strategy.service;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import java.math.BigDecimal;
import java.time.Instant;

public class VolatilityBreakoutLongStrategy implements TradingStrategy {

    private final CandidateScannerService candidateScannerService;
    private final StrategyProperties strategyProperties;

    public VolatilityBreakoutLongStrategy(
            CandidateScannerService candidateScannerService,
            StrategyProperties strategyProperties
    ) {
        this.candidateScannerService = candidateScannerService;
        this.strategyProperties = strategyProperties;
    }

    @Override
    public TradingSignal evaluate(MarketPrice marketPrice) {
        if (marketPrice == null || marketPrice.market() == null || marketPrice.market().isBlank()
                || marketPrice.currentPrice() == null) {
            return hold(null, null, "Market price is not available");
        }

        try {
            TradingCandidate candidate = candidateScannerService.scan(marketPrice.market());
            if (candidate.decision() != CandidateDecision.SELECTED) {
                return hold(marketPrice.market(), marketPrice.currentPrice(), "No volatility breakout long signal: " + candidate.reason());
            }

            BigDecimal targetPrice = candidate.currentPrice() == null ? marketPrice.currentPrice() : candidate.currentPrice();
            return new TradingSignal(
                    marketPrice.market(),
                    SignalType.BUY,
                    candidate.reason(),
                    targetPrice,
                    strategyProperties.getOrderQuantity(),
                    candidate.scannedAt()
            );
        } catch (RuntimeException exception) {
            return hold(marketPrice.market(), marketPrice.currentPrice(), "Volatility breakout evaluation failed");
        }
    }

    private TradingSignal hold(String market, BigDecimal price, String reason) {
        return new TradingSignal(
                market,
                SignalType.HOLD,
                reason,
                price,
                BigDecimal.ZERO,
                Instant.now()
        );
    }
}
