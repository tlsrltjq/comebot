package com.giseop.comebot.strategy.service;

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
    private final PositionEntryGuardService positionEntryGuardService;
    private final StrategyMarketSettingsService strategyMarketSettingsService;

    public VolatilityBreakoutLongStrategy(
            CandidateScannerService candidateScannerService,
            PositionEntryGuardService positionEntryGuardService,
            StrategyMarketSettingsService strategyMarketSettingsService
    ) {
        this.candidateScannerService = candidateScannerService;
        this.positionEntryGuardService = positionEntryGuardService;
        this.strategyMarketSettingsService = strategyMarketSettingsService;
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
            if (positionEntryGuardService.shouldBlockEntry(marketPrice.market())) {
                return hold(marketPrice.market(), marketPrice.currentPrice(), "Paper position already exists");
            }

            BigDecimal targetPrice = candidate.currentPrice() == null ? marketPrice.currentPrice() : candidate.currentPrice();
            return new TradingSignal(
                    marketPrice.market(),
                    SignalType.BUY,
                    candidate.reason(),
                    targetPrice,
                    strategyMarketSettingsService.buyQuantity(marketPrice.market(), targetPrice),
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
