package com.giseop.comebot.risk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.giseop.comebot.exchange.ExchangeMode;
import com.giseop.comebot.execution.domain.OrderRequest;
import com.giseop.comebot.execution.domain.OrderSide;
import com.giseop.comebot.portfolio.domain.PaperPosition;
import com.giseop.comebot.portfolio.service.PaperPortfolioService;
import com.giseop.comebot.risk.PositionLimitProperties;
import com.giseop.comebot.risk.domain.RiskDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PositionLimitRiskValidationServiceTest {

    @Test
    void rejectsNewBuyWhenExchangePositionLimitIsReached() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPosition(ExchangeMode.UPBIT, "KRW-NEW")).thenReturn(Optional.empty());
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(
                position("KRW-BTC"),
                position("KRW-ETH"),
                position("KRW-XRP")
        ));
        when(portfolioService.findPositions(ExchangeMode.BINANCE)).thenReturn(List.of());

        var result = new PositionLimitRiskValidationService(new PositionLimitProperties(), portfolioService)
                .validate(ExchangeMode.UPBIT, buy("KRW-NEW"));

        assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
        assertThat(result.reason()).contains("Exchange position limit reached");
    }

    @Test
    void existingPositionBuyDoesNotIncreasePositionCount() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPosition(ExchangeMode.UPBIT, "KRW-BTC")).thenReturn(Optional.of(position("KRW-BTC")));

        var result = new PositionLimitRiskValidationService(new PositionLimitProperties(), portfolioService)
                .validate(ExchangeMode.UPBIT, buy("KRW-BTC"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void rejectsNewBuyWhenTotalPositionLimitIsReached() {
        PositionLimitProperties props = new PositionLimitProperties();
        props.setUpbitMaxPositions(3);
        props.setBinanceMaxPositions(3);
        props.setTotalMaxPositions(2);
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPosition(ExchangeMode.UPBIT, "KRW-NEW")).thenReturn(Optional.empty());
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(position("KRW-BTC")));
        when(portfolioService.findPositions(ExchangeMode.BINANCE)).thenReturn(List.of(position("BTCUSDT")));

        var result = new PositionLimitRiskValidationService(props, portfolioService)
                .validate(ExchangeMode.UPBIT, buy("KRW-NEW"));

        assertThat(result.decision()).isEqualTo(RiskDecision.REJECTED);
        assertThat(result.reason()).contains("Total position limit reached");
    }

    @Test
    void approvesWhenUnderAllLimits() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        when(portfolioService.findPosition(ExchangeMode.UPBIT, "KRW-NEW")).thenReturn(Optional.empty());
        when(portfolioService.findPositions(ExchangeMode.UPBIT)).thenReturn(List.of(position("KRW-BTC")));
        when(portfolioService.findPositions(ExchangeMode.BINANCE)).thenReturn(List.of());

        var result = new PositionLimitRiskValidationService(new PositionLimitProperties(), portfolioService)
                .validate(ExchangeMode.UPBIT, buy("KRW-NEW"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void sellOrdersBypassPositionLimitCheck() {
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);
        OrderRequest sell = new OrderRequest("KRW-BTC", OrderSide.SELL, BigDecimal.ONE, BigDecimal.TEN, Instant.now());

        var result = new PositionLimitRiskValidationService(new PositionLimitProperties(), portfolioService)
                .validate(ExchangeMode.UPBIT, sell);

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    @Test
    void disabledPropertyApprovesEverything() {
        PositionLimitProperties props = new PositionLimitProperties();
        props.setEnabled(false);
        PaperPortfolioService portfolioService = mock(PaperPortfolioService.class);

        var result = new PositionLimitRiskValidationService(props, portfolioService)
                .validate(ExchangeMode.UPBIT, buy("KRW-NEW"));

        assertThat(result.decision()).isEqualTo(RiskDecision.APPROVED);
    }

    private PaperPosition position(String market) {
        return new PaperPosition(market, BigDecimal.ONE, BigDecimal.TEN);
    }

    private OrderRequest buy(String market) {
        return new OrderRequest(market, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, Instant.now());
    }
}
