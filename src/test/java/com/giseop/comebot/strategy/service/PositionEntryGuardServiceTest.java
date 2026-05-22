package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionEntryGuardServiceTest {

    @Test
    void blocksEntryWhenPositionAlreadyExists() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, true).shouldBlockEntry("KRW-BTC")).isTrue();
    }

    @Test
    void doesNotBlockWhenDisabled() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, false).shouldBlockEntry("KRW-BTC")).isFalse();
    }

    @Test
    void doesNotBlockWhenPositionQuantityIsZero() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", BigDecimal.ZERO, new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, true).shouldBlockEntry("KRW-BTC")).isFalse();
    }

    @Test
    void allowsReentryWhenPositionIsInSufficientProfit() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));
        PositionEntryGuardService guard = serviceWithMinProfit(portfolioService, new BigDecimal("0.5"));

        // currentPrice=101 → +1% ≥ 0.5% → allow
        assertThat(guard.shouldBlockEntry(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("101"))).isFalse();
    }

    @Test
    void blocksReentryWhenPositionIsNotInSufficientProfit() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));
        PositionEntryGuardService guard = serviceWithMinProfit(portfolioService, new BigDecimal("0.5"));

        // currentPrice=100.4 → +0.4% < 0.5% → block
        assertThat(guard.shouldBlockEntry(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100.4"))).isTrue();
    }

    @Test
    void allowsReentryWhenMinProfitRateIsZeroDisabled() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));
        PositionEntryGuardService guard = serviceWithMinProfit(portfolioService, BigDecimal.ZERO);

        // minProfitRate=0 (disabled) → always allow re-entry
        assertThat(guard.shouldBlockEntry(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("99"))).isFalse();
    }

    @Test
    void allowsFirstEntryWithNoPosition() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of());
        PositionEntryGuardService guard = serviceWithMinProfit(portfolioService, new BigDecimal("0.5"));

        assertThat(guard.shouldBlockEntry(ExchangeMode.UPBIT, "KRW-BTC", new BigDecimal("100"))).isFalse();
    }

    private PositionEntryGuardService service(PaperPortfolioService portfolioService, boolean enabled) {
        StrategyEntryProperties properties = new StrategyEntryProperties();
        properties.setPreventReentryWithPosition(enabled);
        return new PositionEntryGuardService(portfolioService, properties);
    }

    private PositionEntryGuardService serviceWithMinProfit(PaperPortfolioService portfolioService, BigDecimal minProfitRate) {
        StrategyEntryProperties properties = new StrategyEntryProperties();
        properties.setMinReentryProfitRate(minProfitRate);
        return new PositionEntryGuardService(portfolioService, properties);
    }
}
