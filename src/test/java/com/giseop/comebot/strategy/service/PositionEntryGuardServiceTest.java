package com.giseop.comebot.strategy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionEntryGuardServiceTest {

    @Test
    void blocksEntryWhenPositionAlreadyExists() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions()).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, true).shouldBlockEntry("KRW-BTC")).isTrue();
    }

    @Test
    void doesNotBlockWhenDisabled() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions()).thenReturn(List.of(
                new PaperPosition("KRW-BTC", new BigDecimal("0.01"), new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, false).shouldBlockEntry("KRW-BTC")).isFalse();
    }

    @Test
    void doesNotBlockWhenPositionQuantityIsZero() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPositions()).thenReturn(List.of(
                new PaperPosition("KRW-BTC", BigDecimal.ZERO, new BigDecimal("100"))
        ));

        assertThat(service(portfolioService, true).shouldBlockEntry("KRW-BTC")).isFalse();
    }

    private PositionEntryGuardService service(PaperPortfolioService portfolioService, boolean enabled) {
        StrategyEntryProperties properties = new StrategyEntryProperties();
        properties.setPreventReentryWithPosition(enabled);
        return new PositionEntryGuardService(portfolioService, properties);
    }
}
