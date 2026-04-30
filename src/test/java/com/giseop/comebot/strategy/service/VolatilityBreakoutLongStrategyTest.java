package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.config.StrategyProperties;
import com.giseop.comebot.strategy.candidate.CandidateScannerProperties;
import com.giseop.comebot.market.domain.MarketPrice;
import com.giseop.comebot.strategy.candidate.CandidateDecision;
import com.giseop.comebot.strategy.candidate.CandidateScannerService;
import com.giseop.comebot.strategy.candidate.TradingCandidate;
import com.giseop.comebot.strategy.domain.SignalType;
import com.giseop.comebot.strategy.domain.TradingSignal;
import com.giseop.comebot.strategy.indicator.MarketTrend;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VolatilityBreakoutLongStrategyTest {

    private CandidateScannerService candidateScannerService;
    private PositionEntryGuardService positionEntryGuardService;
    private StrategyMarketOverrideProperties overrideProperties;
    private VolatilityBreakoutLongStrategy strategy;

    @BeforeEach
    void setUp() {
        candidateScannerService = mock(CandidateScannerService.class);
        positionEntryGuardService = mock(PositionEntryGuardService.class);
        overrideProperties = new StrategyMarketOverrideProperties();
        StrategyProperties strategyProperties = new StrategyProperties();
        strategyProperties.setOrderQuantity(new BigDecimal("0.002"));
        strategy = new VolatilityBreakoutLongStrategy(
                candidateScannerService,
                positionEntryGuardService,
                new StrategyMarketSettingsService(strategyProperties, new CandidateScannerProperties(), overrideProperties)
        );
    }

    @Test
    void selectedCandidateCreatesBuySignal() {
        when(candidateScannerService.scan("KRW-BTC")).thenReturn(candidate(CandidateDecision.SELECTED));

        TradingSignal signal = strategy.evaluate(marketPrice("KRW-BTC", "100"));

        assertThat(signal.signalType()).isEqualTo(SignalType.BUY);
        assertThat(signal.market()).isEqualTo("KRW-BTC");
        assertThat(signal.targetPrice()).isEqualByComparingTo("100");
        assertThat(signal.quantity()).isEqualByComparingTo("0.002");
        assertThat(signal.reason()).isEqualTo("Volatility long candidate selected");
    }

    @Test
    void skippedCandidateCreatesHoldSignal() {
        when(candidateScannerService.scan("KRW-BTC")).thenReturn(candidate(CandidateDecision.SKIPPED));

        TradingSignal signal = strategy.evaluate(marketPrice("KRW-BTC", "100"));

        assertThat(signal.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.quantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(signal.reason()).contains("No volatility breakout long signal");
    }

    @Test
    void nullMarketPriceCreatesHoldSignal() {
        TradingSignal signal = strategy.evaluate(null);

        assertThat(signal.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.reason()).isEqualTo("Market price is not available");
    }

    @Test
    void scannerFailureCreatesHoldSignal() {
        when(candidateScannerService.scan("KRW-BTC")).thenThrow(new IllegalStateException("failed"));

        TradingSignal signal = strategy.evaluate(marketPrice("KRW-BTC", "100"));

        assertThat(signal.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.reason()).isEqualTo("Volatility breakout evaluation failed");
    }

    @Test
    void existingPaperPositionCreatesHoldSignal() {
        when(candidateScannerService.scan("KRW-BTC")).thenReturn(candidate(CandidateDecision.SELECTED));
        when(positionEntryGuardService.shouldBlockEntry("KRW-BTC")).thenReturn(true);

        TradingSignal signal = strategy.evaluate(marketPrice("KRW-BTC", "100"));

        assertThat(signal.signalType()).isEqualTo(SignalType.HOLD);
        assertThat(signal.reason()).isEqualTo("Paper position already exists");
    }

    @Test
    void marketOverrideOrderQuantityIsUsed() {
        StrategyMarketOverrideProperties.MarketOverride override = new StrategyMarketOverrideProperties.MarketOverride();
        override.setOrderQuantity(new BigDecimal("0.003"));
        overrideProperties.setMarkets(java.util.Map.of("KRW-BTC", override));
        when(candidateScannerService.scan("KRW-BTC")).thenReturn(candidate(CandidateDecision.SELECTED));

        TradingSignal signal = strategy.evaluate(marketPrice("KRW-BTC", "100"));

        assertThat(signal.quantity()).isEqualByComparingTo("0.003");
    }

    private MarketPrice marketPrice(String market, String currentPrice) {
        return new MarketPrice(market, new BigDecimal(currentPrice), Instant.now());
    }

    private TradingCandidate candidate(CandidateDecision decision) {
        return new TradingCandidate(
                "KRW-BTC",
                decision,
                decision == CandidateDecision.SELECTED ? "Volatility long candidate selected" : "Trend is not UP",
                new BigDecimal("100"),
                new BigDecimal("2.5"),
                new BigDecimal("4.0"),
                new BigDecimal("10.0"),
                MarketTrend.UP,
                Instant.now()
        );
    }
}
